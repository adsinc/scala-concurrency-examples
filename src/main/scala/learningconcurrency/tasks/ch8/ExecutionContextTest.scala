package learningconcurrency.tasks.ch8

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import akka.pattern._
import akka.util.Timeout
import akka.util.Timeout.durationToTimeout
import learningconcurrency.tasks._
import learningconcurrency.tasks.ch8.DispatcherActor.{Completed, Execute, Failed}

import scala.collection.mutable
import scala.concurrent.duration.{DurationLong, FiniteDuration}
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ActorExecutionContext(executorCount: Int, timeout: FiniteDuration = 30.seconds) extends ExecutionContext {
  private val actorSystem = ActorSystem("ActorExecutionContext")
  private val dispatcher = actorSystem.actorOf(Props(new DispatcherActor(executorCount)))
  private implicit val waitTimeout: Timeout = timeout

  def execute(runnable: Runnable): Unit = {
    import scala.concurrent.ExecutionContext.Implicits.global
    dispatcher ? Execute(runnable) onComplete {
      case Failure(t) => reportFailure(t)
      case Success(Failed(t)) => reportFailure(t)
      case _ =>
    }
  }

  def reportFailure(cause: Throwable): Unit =
    log(s"Error on task execute: $cause")

  def shutdown(): Unit = actorSystem.terminate()
}


class DispatcherActor(executorCount: Int) extends Actor {
  private val freeExecutors = mutable.Queue[ActorRef]()
  private val waitingTasks = mutable.Queue[Execute]()
  private val runningExecutors = mutable.Set[ActorRef]()

  override def preStart(): Unit =
    for (_ <- 1 to executorCount) freeExecutors.enqueue(context.actorOf(Props[ExecutorActor]))

  def receive: Receive = {
    case task@Execute(_) =>
      waitingTasks.enqueue(task)
      tryStartTask()
    case msg@Completed =>
      context.parent forward msg
      freeSender()
      tryStartTask()
    case msg@Failed(_) =>
      context.parent forward msg
      freeSender()
      tryStartTask()
  }

  private def freeSender(): Unit = {
    runningExecutors -= sender()
    freeExecutors enqueue sender()
  }

  private def tryStartTask(): Unit = {
    if (freeExecutors.nonEmpty && waitingTasks.nonEmpty) {
      val executor = freeExecutors.dequeue()
      executor ! waitingTasks.dequeue()
      runningExecutors += executor
    }
  }
}

object DispatcherActor {

  case class Execute(task: Runnable)

  case object Completed

  case class Failed(t: Throwable)
}

class ExecutorActor extends Actor {
  val log = Logging(context.system, this)

  def receive: Receive = {
    case Execute(task) =>
      Try(task.run()) match {
        case Success(()) =>
          sender() ! Completed
          log.info("Task completed")
        case Failure(e) =>
          sender() ! Failed(e)
          log.info(s"Task failed: ${e.getMessage}")
      }
  }
}

object ExecutionContextTest extends App {
  implicit val executionContext: ActorExecutionContext = new ActorExecutionContext(3)

  for(i <- 1 to 2) Future {
    println(s"Task $i")
  }

  executionContext.execute { () => throw new IllegalArgumentException("Error!") }

  Thread.sleep(1000)

  executionContext.shutdown()
}

package learningconcurrency.tasks.ch8

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import learningconcurrency.tasks.ch8.DispatcherActor.{Completed, Execute, Failed}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util.{Failure, Success, Try}

class ActorExecutionContext(executorCount: Int) extends ExecutionContext {
  private val actorSystem = ActorSystem("ActorExecutionContext")
  private val dispatcher = actorSystem.actorOf(Props(new DispatcherActor(executorCount)))

  def execute(runnable: Runnable): Unit = dispatcher ! Execute(runnable)

  def reportFailure(cause: Throwable): Unit = println(s"Error on task execute: $cause")

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
    case Completed =>
      runningExecutors -= sender()
      freeExecutors.enqueue(sender())
      tryStartTask()
    case Failed(t) =>
      ???
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

  for(i <- 1 to 10) Future {
    println(s"Task $i")
  }

  Thread.sleep(1000)

  executionContext.shutdown()
}

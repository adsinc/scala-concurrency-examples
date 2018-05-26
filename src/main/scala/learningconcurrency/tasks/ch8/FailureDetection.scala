package learningconcurrency.tasks.ch8

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Timers}
import learningconcurrency.tasks.ch8.FailureDetector.{ActorIdentify, Failed, Identify, IdentityTick}

import scala.concurrent.duration.{DurationLong, FiniteDuration}

class FailureDetector(watchActors: Seq[ActorRef],
                      checkPeriod: FiniteDuration = 1.second,
                      maxAnswerDuration: FiniteDuration = 100.millis
                     ) extends Actor with Timers with ActorLogging {

  private val actorTimers = watchActors.map(ref => (ref, s"Timer:$ref")).toMap

  override def preStart(): Unit = {
    timers.startPeriodicTimer("Send Identity Timer", IdentityTick, checkPeriod)
  }

  def receive: Receive = {
    case IdentityTick =>
      log.info("Checking actors")
      actorTimers.foreach {
        case (ref, timerKey) =>
          ref ! Identify
          timers.startSingleTimer(timerKey, Failed(ref), maxAnswerDuration)
      }
    case msg@Failed(_) =>
      context.parent forward msg
    case ActorIdentify =>
      timers cancel actorTimers(sender())
  }
}

class Worker(failChance: Double = 0.05) extends Actor {
  def receive: Receive = {
    case Identify =>
      if(math.random() > failChance)
        sender() ! ActorIdentify
  }
}

class FailureDetectorParent(workersCount: Int = 10) extends Actor with ActorLogging {

  override def preStart(): Unit = {
    val workers = (1 to workersCount) map (_ => context.actorOf(Props(new Worker)))
    context.actorOf(Props(new FailureDetector(workers)))
  }

  def receive: Receive = {
    case Failed(ref) =>
      log.info(s"Actor $ref failed")
  }
}

object FailureDetector {
  object IdentityTick
  object Identify
  object ActorIdentify
  case class Failed(actor: ActorRef)
}

object FailureDetection extends App {
  val system = ActorSystem("FailureDetector")
  system.actorOf(Props(new FailureDetectorParent()))
}

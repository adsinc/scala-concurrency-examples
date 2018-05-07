package learningconcurrency.tasks.ch8

import akka.actor._
import akka.event.Logging
import akka.pattern._
import akka.util.Timeout
import learningconcurrency.tasks.ch8.TimeActor._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.DurationLong

class TimeActor extends Actor with Timers {
  val log = Logging(context.system, this)
  var timerId = 0
  private val timeouts = Map[Int, ActorRef]()

  def receive: Receive = onMessage(timeouts)

  private def onMessage(timeouts: Map[Int, ActorRef]): Receive = {
    case Register(t) =>
      log.debug(s"Registered timeout $timerId for $t ms")
      context become onMessage(timeouts + (timerId -> sender()))
      timers.startSingleTimer(timerId, Tick(timerId), t.millis)
      timerId += 1
    case Tick(id) =>
      timeouts.get(id) match {
        case Some(src) => src ! Timeout
        case None => log.error("No timer ")
      }
      context become onMessage(timeouts - id)
  }
}

object TimeActor {
  final case class Register(t: Long)
  case class Tick(timerNumber: Int)
  object Timeout
}

object TimerActorRunner extends App {
  lazy val actorSystem = ActorSystem("TimeActorSystem")
  val timer = actorSystem.actorOf(Props[TimeActor], "timeActor")

  implicit val timeout: Timeout = akka.util.Timeout(5.second)
  timer ? Register(1000) onComplete { e => println(e)}
  timer ? Register(1500) onComplete { e => println(e)}
  timer ? Register(4000) onComplete { e => println(e)}

  Thread.sleep(5000)
  actorSystem.terminate()
}

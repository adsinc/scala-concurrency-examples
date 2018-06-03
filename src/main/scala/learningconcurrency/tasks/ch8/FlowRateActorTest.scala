package learningconcurrency.tasks.ch8

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props, Timers}
import learningconcurrency.tasks.ch8.TimeActor.Tick

import scala.collection.immutable.Queue
import scala.concurrent.duration.DurationDouble
import scala.util.Random

class FlowRateActor(dest: ActorRef, threshold: Int = 5) extends Actor with Timers {

  override def preStart(): Unit =
    timers.startPeriodicTimer("MessageCountIntervalTimer", Tick, 1.seconds)

  def receive: Receive = beforeThreshold(0)

  def beforeThreshold(receivedCount: Int): Receive = {
    case Tick =>
      context become beforeThreshold(0)
    case msg =>
      dest ! msg
      val newReceivedCount = receivedCount + 1
      if(newReceivedCount >= threshold) {
        context become afterThreshold()
      } else
        context become beforeThreshold(newReceivedCount)
  }

  def afterThreshold(queue: Queue[Any] = Queue.empty): Receive = {
    case Tick =>
      context become beforeThreshold(0)
      for {
        msg <- queue
      } self ! msg
    case msg =>
      context become afterThreshold(queue enqueue msg)
  }
}

object FlowRateActor {
  object Tick
}

class ConsumerActor extends Actor with ActorLogging {
  def receive: Receive = {
    case msg => log.info(s"Received: $msg")
  }
}

object FlowRateActorTest extends App {
  val actorSystem = ActorSystem()
  val consumer = actorSystem.actorOf(Props[ConsumerActor])
  val flowRateActor = actorSystem.actorOf(Props(new FlowRateActor(consumer)))

  for(i <- 1 to 10) {
    1 to (Random.nextInt(9) + 1) foreach  {k =>
      flowRateActor ! s"ping $k times in $i sequence"
    }
    Thread.sleep(1000)
  }

  Thread.sleep(2000)

  actorSystem.terminate()
}

package learningconcurrency.tasks.ch8

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

import scala.collection.immutable.TreeMap

class Sequencer(dest: ActorRef) extends Actor {
  def receive: Receive = onMessage(0)

  private def onMessage(nextNumber: Long, derived: TreeMap[Long, Any] = TreeMap.empty): Receive = {
    case msg@(num: Long, _) =>
      if (num == nextNumber) {
        dest ! msg
        var newNextNumber = nextNumber + 1
        context become onMessage(newNextNumber)
      } else {
        context become onMessage(nextNumber, derived + msg)
      }
    case msg => dest ! msg
  }
}

class ConsumerActor extends Actor with ActorLogging {
  def receive: Receive = {
    case msg => log.info(s"Received: $msg")
  }
}

object FlowRateActorTest extends App {
  val actorSystem = ActorSystem()
  val consumer = actorSystem.actorOf(Props[ConsumerActor])
  val sequencer = actorSystem.actorOf(Props(new Sequencer(consumer)))


  actorSystem.terminate()
}

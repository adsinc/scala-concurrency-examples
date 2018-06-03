package learningconcurrency.tasks.ch8

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}

class Sequencer(dest: ActorRef) extends Actor {
  def receive: Receive = onMessage(0)

  def onMessage(nextNumber: Long): Receive = {
    case (num: Long, msg) => ???
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

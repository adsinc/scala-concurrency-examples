package learningconcurrency.tasks.ch8

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import learningconcurrency.tasks.ch8.Sequencer.SequencedMsg

import scala.annotation.tailrec
import scala.collection.immutable.TreeMap
import scala.util.Random

class Sequencer(dest: ActorRef) extends Actor with ActorLogging {
  def receive: Receive = onMessage(0)

  private def onMessage(nextNumber: Long, derived: TreeMap[Long, SequencedMsg] = TreeMap.empty): Receive = {
    case msg@SequencedMsg(num, _) =>
      if(num < nextNumber) {
        log.error(s"Repeated message number. Expected: $nextNumber, actual: $num")
      } else if (num == nextNumber) {
        dest ! msg
        val (newNextNumber, newDerived) = trySendDerived(nextNumber + 1, derived)
        context become onMessage(newNextNumber, newDerived)
      } else {
        context become onMessage(nextNumber, derived + (msg.seqNum -> msg))
      }
    case msg => dest ! msg
  }

  @tailrec
  private def trySendDerived(nextNumber: Long, derived: TreeMap[Long, SequencedMsg]): (Long, TreeMap[Long, SequencedMsg]) = {
    if(derived.isEmpty || derived.firstKey > nextNumber)
      (nextNumber, derived)
    else {
      dest ! derived.head._2
      trySendDerived(nextNumber + 1, derived.tail)
    }
  }
}

object Sequencer {
  case class SequencedMsg(seqNum: Long, msg: Any)
}

object SequencerActorTest extends App {
  val actorSystem = ActorSystem()
  val consumer = actorSystem.actorOf(Props[ConsumerActor])
  val sequencer = actorSystem.actorOf(Props(new Sequencer(consumer)))

  for {
    i <- Random.shuffle(0 to 50)
  } sequencer ! SequencedMsg(i, s"Message $i")

  Thread.sleep(1000)

  actorSystem.terminate()
}

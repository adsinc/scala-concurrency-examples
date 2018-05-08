package learningconcurrency.tasks.ch8

import akka.actor.{Actor, ActorRef, ActorSystem}
import akka.event.Logging
import learningconcurrency.tasks.ch8.AccountActor.{Deposit, Send}

class AccountActor(val initialSum: Long) extends Actor {
  val log = Logging(context.system, this)

  def receive: Receive = onMessage(initialSum)

  def onMessage(sum: Long): Receive = {
    case Send(amount, dest) =>
      log.info(s"Trying to send $amount")
      if (amount <= sum && amount > 0) {
        context become onMessage(sum - amount)
        dest ! Deposit(amount)
      } else {
        log.error("No money")
      }
    case Deposit(amount) =>
      log.info(s"Received $amount")
      context become onMessage(sum + amount)
  }
}

object AccountActor {

  case class Send(amount: Long, dest: ActorRef)

  case class Deposit(amount: Long)

}

object Account extends App {
  val actorSystem = ActorSystem("Accounts")

  // todo check accounts equal, generate accounts and send money many times
  // todo check killing actor

  actorSystem.terminate()
}

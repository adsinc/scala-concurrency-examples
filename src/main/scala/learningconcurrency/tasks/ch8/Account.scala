package learningconcurrency.tasks.ch8

import akka.actor._
import akka.event.Logging
import akka.pattern._
import akka.util.Timeout
import learningconcurrency.tasks.ch8.AccountActor.{AccountBalance, CheckBalance, Deposit, Send}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.{Duration, DurationDouble}
import scala.concurrent.{Await, Future}

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
      if(sender() != self) {
        log.info(s"Received $amount")
        context become onMessage(sum + amount)
      } else {
        log.info(s"Ignore deposit $amount to self")
      }
    case CheckBalance =>
      sender() ! AccountBalance(sum)
  }
}

object AccountActor {

  case class Send(amount: Long, dest: ActorRef)

  case class Deposit(amount: Long)

  object CheckBalance

  case class AccountBalance(sum: Long)

  def props(sum: Long) = Props(new AccountActor(sum))
}

object Account extends App {
  val actorSystem = ActorSystem("Accounts")

  val accounts = 1 to 10 map (i => actorSystem.actorOf(AccountActor.props(1000), s"account-$i"))

  // todo send money many times
  // todo check killing actor

  implicit val timeout: Timeout = akka.util.Timeout(2.seconds)
  def getBalance(ref: ActorRef): Future[Long] =
    ref ? CheckBalance map {
      case AccountBalance(sum) => sum
    }

  val totalSum = Await.result(Future.sequence(accounts map getBalance), Duration.Inf).sum
  println(s"Total sum $totalSum")

  actorSystem.terminate()
}


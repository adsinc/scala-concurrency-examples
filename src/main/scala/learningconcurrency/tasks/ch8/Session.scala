package learningconcurrency.tasks.ch8

import akka.actor.{Actor, ActorRef, ActorSystem, Props}
import akka.event.Logging
import learningconcurrency.tasks.ch8.SessionActor.{StartSession, StopSession}

import scala.concurrent.Await
import scala.concurrent.duration.Duration

class SessionActor(password: String, r: ActorRef) extends Actor {
  val log = Logging(context.system, this)

  def receive: Receive = noSession

  def noSession: Receive = {
    case StartSession(pass) =>
      if (pass == password) {
        log.info("Session started")
        context become sessionStarted
      } else
        log.error("Incorrect password")
    case msg =>
      log.error(s"No session. Skipping message: $msg")
  }

  def sessionStarted: Receive = {
    case StopSession =>
      log.info("Session stopped")
      context become noSession
    case msg@_ =>
      log.info(s"Forward message: $msg")
      r forward msg
  }
}

object SessionActor {

  case class StartSession(password: String)

  case object StopSession

  def props(password: String, r: ActorRef) = Props(new SessionActor(password, r))
}

class EchoActor extends Actor {
  val log = Logging(context.system, this)

  def receive: Receive = {
    case msg => log.info(s"Message received: $msg")
  }
}

object EchoActor {
  def props = Props(new EchoActor())
}

object Session extends App {
  val actorSystem = ActorSystem("Session")

  val echoActor = actorSystem.actorOf(EchoActor.props)
  val sessionActor = actorSystem.actorOf(SessionActor.props("secret", echoActor))

  sessionActor ! "Hello"

  sessionActor ! StartSession("qwerty")
  sessionActor ! "Hello"

  sessionActor ! StartSession("secret")
  sessionActor ! "Hello"

  sessionActor ! StopSession
  sessionActor ! "Hello"

  Await.ready(actorSystem.terminate(), Duration.Inf)
}

package learningconcurrency.tasks.ch9.ftpbrowser.client

import akka.actor.{Actor, ActorRef, Identify}
import akka.util.Timeout
import learningconcurrency.tasks.ch9.ftpbrowser.client.FTPClient.FTPClientActor.Start
;

object FTPClient {

  object FTPClientActor {

    case class Start(host: String)

  }

  class FTPClientActor(implicit val timeout: Timeout) extends Actor {
    def receive: Receive = ???

    def unconnected: Receive = {
      case Start(host) =>
        val serverActorPath = s"akka.tcp://FTPServerSystem@$host/user/server"
        val serverActorSel = context.actorSelection(serverActorPath)
        serverActorSel ! Identify(())
        context become connecting(sender())
    }

    def connecting(clientApp: ActorRef): Receive = ???
  }

}

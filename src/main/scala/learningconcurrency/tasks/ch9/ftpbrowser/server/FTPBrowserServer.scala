package learningconcurrency.tasks.ch9.ftpbrowser.server

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern._
import com.typesafe.config.ConfigFactory
import learningconcurrency.tasks.ch9.ftpbrowser.server.FTPBrowserServer.FtpServerActor
import learningconcurrency.tasks.ch9.ftpbrowser.server.FTPBrowserServer.FtpServerActor.{CopyFile, DeleteFile, GetFileList}
import learningconcurrency.tasks.ch9.ftpbrowser.server.FileManagement.FileSystem

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

object FTPBrowserServer {

  object FtpServerActor {

    sealed trait Command

    case class GetFileList(dir: String) extends Command

    case class CopyFile(src: String, dest: String) extends Command

    case class DeleteFile(path: String) extends Command

    def apply(fs: FileSystem) = Props(classOf[FtpServerActor], fs)
  }

  class FtpServerActor(fileSystem: FileSystem) extends Actor with ActorLogging {
    def receive: Receive = {
      case GetFileList(dir) =>
        val filesMap = fileSystem.getFileList(dir)
        val files = filesMap.values.toList
        sender() ! files
      case CopyFile(srcPath, destPath) =>
        Future {
          Try(fileSystem.copyFiles(srcPath, destPath))
        } pipeTo sender()
      case DeleteFile(path) =>
        Future {
          Try(fileSystem.deleteFile(path))
        } pipeTo sender()
    }
  }

}

object FTPServer extends App {
  val fileSystem = new FileSystem(".")
  fileSystem.init()
  val port = args(0).toInt
  val actorSystem = ActorSystem("FTPServerSystem", ConfigFactory.parseResources("server.conf"))
  actorSystem.actorOf(FtpServerActor(fileSystem), "server")
  //  implicit val timeout: Timeout = 1.second
  //
  //  a ? GetFileList(".") map (x => x.asInstanceOf[List[FileInfo]]) foreach { fs =>
  //    fs foreach println
  //  }
  //
  //  Thread.sleep(2000)
  //  actorSystem.terminate()
}

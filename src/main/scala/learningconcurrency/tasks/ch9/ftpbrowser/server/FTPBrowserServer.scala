package learningconcurrency.tasks.ch9.ftpbrowser.server

import java.io.File

import akka.actor.{Actor, ActorLogging, ActorSystem, Props}
import akka.pattern._
import com.typesafe.config.ConfigFactory
import learningconcurrency.tasks.ch9.ftpbrowser.server.FTPBrowserServer.FtpServerActor
import learningconcurrency.tasks.ch9.ftpbrowser.server.FTPBrowserServer.FtpServerActor.{CopyFile, DeleteFile, GetFileList, MakeDirectory}
import learningconcurrency.tasks.ch9.ftpbrowser.server.FileManagement._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.util.Try

object FTPBrowserServer {

  object FtpServerActor {

    sealed trait Command

    case class GetFileList(dir: String) extends Command

    case class CopyFile(src: String, dest: String) extends Command

    case class DeleteFile(path: String) extends Command

    case class MakeDirectory(dirPath: String) extends Command

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
      case MakeDirectory(dirPath) =>
        Future {
          Try(fileSystem.makeDirectory(dirPath))
        } pipeTo sender()
    }
  }
}

object FTPServer extends App {
  val fileSystem = new FileSystem(".")
  fileSystem.init()
  val actorSystem = ActorSystem("FTPServerSystem", ConfigFactory.parseResources("server.conf"))
  actorSystem.actorOf(FtpServerActor(fileSystem), "server")
  val fileEventSubscribtion = fileSystemEvents(".").subscribe { e =>
    e match {
      case FileCreated(path) =>
        fileSystem.files.single(path) = FileInfo(new File(path))
      case FileDeleted(path) =>
        fileSystem.files.single.remove(path)
      case FileModified(path) =>
        fileSystem.files.single(path) = FileInfo(new File(path))
    }
  }
}

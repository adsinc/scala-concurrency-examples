package learningconcurrency.tasks
package ch9.ftpbrowser.client

import java.io.File

import akka.actor._
import akka.pattern._
import akka.util.Timeout
import com.typesafe.config.ConfigFactory
import javax.swing.table.DefaultTableModel
import javax.swing.{SwingUtilities, UIManager}
import learningconcurrency.tasks.ch9.ftpbrowser.client.FTPClient.FTPClientActor.Start
import learningconcurrency.tasks.ch9.ftpbrowser.client.FTPClient.{FTPClientApi, FTPClientFrame, FTPClientLogic}
import learningconcurrency.tasks.ch9.ftpbrowser.server.FTPBrowserServer.FtpServerActor
import learningconcurrency.tasks.ch9.ftpbrowser.server.FTPBrowserServer.FtpServerActor.Command
import learningconcurrency.tasks.ch9.ftpbrowser.server.FileManagement.FileInfo
import rx.lang.scala.Observable

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.swing.BorderPanel.Position._
import scala.swing.event.MousePressed
import scala.swing.{Alignment, BorderPanel, Button, Dialog, Frame, GridPanel, Label, MainFrame, Menu, MenuBar, MenuItem, ScrollPane, SimpleSwingApplication, Table, TextField}
import scala.util.{Failure, Success, Try}

object FTPClient {

  object FTPClientActor {

    case class Start(host: String)

  }

  class FTPClientActor(implicit val timeout: Timeout) extends Actor with ActorLogging {
    def receive: Receive = unconnected

    def unconnected: Receive = {
      case Start(host) =>
        val serverActorPath = s"akka.tcp://FTPServerSystem@$host/user/server"
        val serverActorSel = context.actorSelection(serverActorPath)
        serverActorSel ! Identify(())
        context become connecting(sender())
    }

    def connecting(clientApp: ActorRef): Receive = {
      case ActorIdentity(_, Some(ref)) =>
        clientApp ! true
        context become connected(ref)
      case ActorIdentity(_, None) =>
        clientApp ! false
        context become unconnected
    }

    def connected(serverActor: ActorRef): Receive = {
      case command: Command =>
        (serverActor ? command).pipeTo(sender())
    }
  }

  trait FTPClientApi {
    implicit val timeout: Timeout = 4.seconds
    private val props = Props(new FTPClientActor())
    private val config = ConfigFactory.parseResources("server.conf")
      .withoutPath("akka.remote.netty.tcp.port")
    private val system = ActorSystem("FTPServerSystem", config)
    private val clientActor = system.actorOf(props)

    def host: String

    val connected: Future[Boolean] = {
      val f = clientActor ? FTPClientActor.Start(host)
      f.mapTo[Boolean]
    }

    def getFileList(d: String): Future[(String, Seq[FileInfo])] = {
      val f = clientActor ? FtpServerActor.GetFileList(d)
      f.mapTo[Seq[FileInfo]].map(fs => (d, fs))
    }

    def copyFile(src: String, dest: String): Future[String] = {
      val f = clientActor ? FtpServerActor.CopyFile(src, dest)
      f.mapTo[Try[String]].map(_.get)
    }

    def deleteFile(srcPath: String): Future[String] = {
      val f = clientActor ? FtpServerActor.DeleteFile(srcPath)
      f.mapTo[Try[String]].map(_.get)
    }

    def makeDirectory(dirPath: String): Future[String] = {
      val f = clientActor ? FtpServerActor.MakeDirectory(dirPath)
      f.mapTo[Try[String]].map(_.get)
    }
  }

  abstract class FTPClientFrame extends MainFrame {
    title = "ScalaFTP"

    object menu extends MenuBar {

      object file extends Menu("File") {
        val exit = new MenuItem("Exit ScalaFTP")
        contents += exit
      }

      object help extends Menu("Help") {
        val about = new MenuItem("About")
        contents += about
      }

      contents += file += help
    }

    object status extends BorderPanel {
      val label = new Label("connecting...", null, Alignment.Left)
      layout(new Label("Status: ")) = West
      layout(label) = Center
    }

    class FilePane extends BorderPanel {

      object pathBar extends BorderPanel {
        val label = new Label("Path:")
        val filePath = new TextField(".") {
          editable = false
        }
        val upButton = new Button("^")
        layout(label) = West
        layout(filePath) = Center
        layout(upButton) = East
      }

      object scrollPane extends ScrollPane {
        val columnNames = Array[AnyRef]("Filename", "Size", "Date modified")
        val fileTable = new Table {
          showGrid = true
          model = new DefaultTableModel(columnNames, 0) {
            override def isCellEditable(row: Int, column: Int): Boolean = false
          }
          selection.intervalMode = Table.IntervalMode.Single
        }
        contents = fileTable
      }

      object buttons extends GridPanel(1, 2) {
        val copyButton = new Button("Copy")
        val deleteButton = new Button("Delete")
        val makeDirectoryButton = new Button("Make dir")
        contents += copyButton += deleteButton += makeDirectoryButton
      }

      layout(pathBar) = North
      layout(scrollPane) = Center
      layout(buttons) = South

      var parent: String = "."
      var dirFiles: Seq[FileInfo] = Nil

      def table = scrollPane.fileTable

      def currentPath = pathBar.filePath.text
    }

    object files extends GridPanel(1, 2) {
      val leftPane = new FilePane
      val rightPane = new FilePane
      contents += leftPane += rightPane

      def opposite(pane: FilePane) =
        if (pane eq leftPane) rightPane else leftPane
    }

    contents = new BorderPanel {
      layout(menu) = North
      layout(files) = Center
      layout(status) = South
    }
  }

  trait FTPClientLogic {
    self: FTPClientFrame with FTPClientApi =>

    import learningconcurrency.tasks.ch6.Browser.ButtonOpts

    def swing(body: => Unit) = {
      val r = new Runnable {
        def run(): Unit = body
      }
      SwingUtilities.invokeLater(r)
    }

    connected.onComplete {
      case Failure(t) =>
        swing(status.label.text = s"Could not connect: $t")
      case Success(false) =>
        swing(status.label.text = s"Could not find server.")
      case Success(true) =>
        swing {
          status.label.text = "Connected!"
          refreshPane(files.leftPane)
          refreshPane(files.rightPane)
        }
    }

    def refreshPane(pane: FilePane): Unit = {
      val dir = pane.pathBar.filePath.text
      getFileList(dir) onComplete {
        case Success((directory, files)) =>
          swing(updatePane(pane, directory, files))
        case Failure(t) =>
          swing(status.label.text = s"Could not update pane: $t")
      }
    }

    def updatePane(pane: FilePane, dir: String, files: Seq[FileInfo]) = {
      val table = pane.scrollPane.fileTable
      table.model match {
        case d: DefaultTableModel =>
          d.setRowCount(0)
          pane.parent =
            if (dir == ".") "."
            else dir.take(dir.lastIndexOf(File.separator))
          pane.dirFiles = files.sortBy(!_.isDir)
          for (f <- pane.dirFiles) d.addRow(f.toRow)
      }
    }

    implicit class TableOps(val self: Table) {
      def rowDoubleClicks: Observable[Int] = Observable[Int] { sub =>
        self.listenTo(self.mouse.clicks)
        self.reactions += {
          case MousePressed(_, _, _, 2, _) =>
            sub.onNext(self.peer.getSelectedRow)
        }
      }
    }

    def setupPane(pane: FilePane): Unit = {
      val fileClicks = pane.table.rowDoubleClicks.map(row => pane.dirFiles(row))
      fileClicks.filter(_.isDir).subscribe { fileInfo =>
        pane.pathBar.filePath.text += File.separator + fileInfo.name
        refreshPane(pane)
      }

      pane.pathBar.upButton.clicks.subscribe { _ =>
        pane.pathBar.filePath.text = pane.parent
        refreshPane(pane)
      }

      def rowActions(button: Button): Observable[FileInfo] =
        button.clicks
          .map(_ => pane.table.peer.getSelectedRow)
          .filter(_ != -1)
          .map(row => pane.dirFiles(row))

      rowActions(pane.buttons.copyButton)
        .map(info => (info, files.opposite(pane).currentPath))
        .subscribe { t =>
          val (info, destDir) = t
          val dest = s"$destDir${File.separator}${info.name}"
          copyFile(info.path, dest) onComplete {
            case Success(s) => changeStatus(s"File copied: $s")
            case Failure(t) => changeStatus(s"Can't copy file: $t")
          }
        }

      rowActions(pane.buttons.deleteButton)
        .subscribe { info =>
          deleteFile(info.path) onComplete {
            case Success(s) => changeStatus(s"File deleted: $s")
            case Failure(t) => changeStatus(s"Can't delete file: $t")
          }
        }

      pane.buttons.makeDirectoryButton
        .clicks
        .subscribe { _ =>
          Dialog.showInput(
            parent = pane,
            message = "Enter directory name",
            title = "Make directory",
            messageType = Dialog.Message.Plain,
            initial = ""
          )
            .map(dirName => s"${pane.currentPath}${File.separator}$dirName")
            .map(makeDirectory)
            .foreach(_.onComplete {
              case Success(s) => changeStatus(s"Directory created: $s")
              case Failure(t) => changeStatus(s"Can't create directory $t")
            })
        }

      def changeStatus(msg: String): Unit = swing {
        status.label.text = msg
        refreshPane(pane)
        refreshPane(files.opposite(pane))
      }
    }

    setupPane(files.leftPane)
    setupPane(files.rightPane)
  }

}

object FTPClientMain extends SimpleSwingApplication {
  try {
    UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName)
  } catch {
    case e: Exception =>
      // ignore
      log(s"could not change look&feel: $e")
  }

  def top: Frame = new FTPClientFrame with FTPClientApi with FTPClientLogic {
    def host: String = "127.0.0.1:8000"
  }
}

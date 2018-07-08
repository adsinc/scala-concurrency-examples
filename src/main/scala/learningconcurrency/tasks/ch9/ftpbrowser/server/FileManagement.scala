package learningconcurrency.tasks.ch9.ftpbrowser.server

import java.io.File
import java.nio.file.Files

import org.apache.commons.io.FileUtils
import org.apache.commons.io.filefilter.TrueFileFilter
import org.apache.commons.io.monitor.{FileAlterationListenerAdaptor, FileAlterationMonitor, FileAlterationObserver}
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.stm.{TMap, Txn, atomic}
import scala.sys.error

object FileManagement {

  sealed trait State {
    def inc: State = error("unsupported operation")

    def dec: State = error("unsupported operation")
  }

  case object Created extends State

  case object Idle extends State {
    override def inc: State = Copying(1)
  }

  case class Copying(n: Int) extends State {
    override def inc: State = Copying(n + 1)

    override def dec: State = if (n > 1) Copying(n - 1) else Idle

  }

  case object Deleted extends State

  case class FileInfo(path: String, name: String, parent: String, modified: String,
                      isDir: Boolean, size: Long, state: State) {
    def toRow: Array[AnyRef] = Array[AnyRef](
      name, if (isDir) "" else s"${size / 1000}kb", modified
    )
  }

  object FileInfo {
    def apply(file: File): FileInfo = {
      require(file.exists(), s"File $file not exists")
      val path = file.toPath
      FileInfo(
        path = file.getAbsolutePath,
        name = file.getName,
        parent = file.getParent,
        modified = Files.getLastModifiedTime(path).toString,
        isDir = file.isDirectory,
        size = Files.size(path),
        state = Idle
      )
    }

    def creating(file: File, size: Long): FileInfo = {
      require(!file.exists(), s"File $file already exists")
      FileInfo(
        path = file.getAbsolutePath,
        name = file.getName,
        parent = file.getParent,
        modified = "n/a",
        isDir = file.isDirectory,
        size = size,
        state = Created
      )
    }
  }

  class FileSystem(val rootPath: String) {
    val files: TMap[String, FileInfo] = TMap[String, FileInfo]()

    def init(): Unit = atomic { implicit txn =>
      import scala.collection.JavaConverters._
      files.clear()
      val rootDir = new File(rootPath)
      val all = TrueFileFilter.INSTANCE
      val fileIterator = FileUtils.iterateFilesAndDirs(rootDir, all, all).asScala
      for (file <- fileIterator) {
        val info = FileInfo(file)
        files(info.path) = info
      }
    }

    def getFileList(dir: String): collection.Map[String, FileInfo] =
      atomic { implicit txn =>
        files.filter(_._2.parent == dir)
      }

    def copyFiles(src: String, dest: String) = atomic { implicit txn =>
      val srcFile = new File(src)
      val destFile = new File(dest)
      val info = files(src)
      if (files.contains(dest)) sys.error(s"Destination file $dest exists.")
      info.state match {
        case Idle | Copying(_) =>
          files(srcFile.getAbsolutePath) = info.copy(state = info.state.inc)
          files(destFile.getAbsolutePath) = FileInfo.creating(destFile, info.size)
          Txn.afterCommit(_ => copyOnDisk(srcFile, destFile))
          src
      }
    }

    private def copyOnDisk(srcFile: File, dstFile: File): Unit = {
      FileUtils.copyFile(srcFile, dstFile)
      atomic { implicit txn =>
        val info = files(srcFile.getAbsolutePath)
        files(srcFile.getAbsolutePath) = info.copy(state = info.state.dec)
        files(dstFile.getAbsolutePath) = FileInfo(dstFile)
      }
    }

    def deleteFile(srcPath: String): String = atomic { implicit txn =>
      val info = files(srcPath)
      info.state match {
        case Idle =>
          files(srcPath) = info.copy(state = Deleted)
          Txn.afterCommit { _ =>
            FileUtils.forceDelete(new File(info.path))
            files.single.remove(srcPath)
          }
          srcPath
      }
    }

    def makeDirectory(dirPath: String): String = atomic { implicit txn =>
      val dirFile = new File(dirPath)
      if (files.contains(dirPath)) sys.error(s"Destination directory $dirPath exists.")
      files(dirPath) = FileInfo.creating(dirFile, 0L)
      Txn.afterCommit { _ =>
        FileUtils.forceMkdir(dirFile)
        files.single(dirPath) = FileInfo(dirFile)
      }
      dirPath
    }
  }

  sealed trait FileEvent

  case class FileCreated(path: String) extends FileEvent

  case class FileDeleted(path: String) extends FileEvent

  case class FileModified(path: String) extends FileEvent

  def fileSystemEvents(rootPath: String): Observable[FileEvent] =
    Observable { obs =>
      val fileMonitor = new FileAlterationMonitor(1000)
      val fileObs = new FileAlterationObserver(rootPath)
      val fileLis = new FileAlterationListenerAdaptor {
        override def onFileCreate(file: File): Unit =
          obs.onNext(FileCreated(file.getPath))

        override def onFileChange(file: File): Unit =
          obs.onNext(FileModified(file.getPath))

        override def onFileDelete(file: File): Unit =
          obs.onNext(FileDeleted(file.getPath))

        override def onDirectoryCreate(directory: File): Unit =
          obs.onNext(FileCreated(directory.getPath))

        override def onDirectoryChange(directory: File): Unit =
          obs.onNext(FileModified(directory.getPath))

        override def onDirectoryDelete(directory: File): Unit =
          obs.onNext(FileDeleted(directory.getPath))
      }
      fileObs.addListener(fileLis)
      fileMonitor.addObserver(fileObs)
      fileMonitor.start()

      Subscription(fileMonitor.stop())
    }

}

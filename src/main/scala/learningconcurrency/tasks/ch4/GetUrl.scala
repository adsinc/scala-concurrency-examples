package learningconcurrency.tasks.ch4

import java.util.{Timer, TimerTask}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise, _}
import scala.io.{Source, StdIn}
import scala.util.{Success, Failure}

object GetUrl extends App {

  def timeout(time: Long): Future[Unit] = {
    val timer = new Timer()
    val p = Promise[Unit]
    timer.schedule(new TimerTask {
      def run(): Unit = {
        p success ()
        timer.cancel()
      }
    }, time)
    p.future
  }

  def readUrl: Future[String] = Future {
    blocking {
      println("Enter url")
      StdIn.readLine()
    }
  }

  def loadUrl(url: String): Future[String] = Future {
      Source.fromURL(url, "UTF-8").getLines() mkString("\n", "\n", "")
  }

  def loadUrlWithWait(url: String): Future[String] = {
    val p = Promise[String]
    loadUrl(url) onComplete p.complete
    def waiting(): Unit =
      if(!p.isCompleted) {
        timeout(50) foreach { _ =>
          print(".")
          waiting()
        }
      }

    waiting()
    p.future
  }

  val result = readUrl flatMap loadUrlWithWait andThen {
    case Success(doc) => println(doc)
    case Failure(e) => println(s"Error: $e")
  }

  Await.ready(result, Duration.Inf)
}

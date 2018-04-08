package learningconcurrency.tasks.ch6

import javax.swing.SwingUtilities
import rx.lang.scala.{Observable, Scheduler, Subscription}
import rx.schedulers.Schedulers

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationDouble
import scala.io.Source
import scala.swing.event.{ButtonClicked, ValueChanged}
import scala.swing.{BorderPanel, Button, Dimension, Frame, Label, MainFrame, SimpleSwingApplication, TextArea, TextField}

object Browser extends SimpleSwingApplication {
  val swingScheduler = new Scheduler {
    val asJavaScheduler: rx.Scheduler = Schedulers.from((command: Runnable) => SwingUtilities.invokeLater(command))
  }

  abstract class BrowserFrame extends MainFrame {
    title = "Mini browser"
    val specUrl = "https://www.w3.org/Addressing/URL/url-spec.txt"
    val urlField = new TextField(specUrl)
    val pageField = new TextArea()
    val button: Button = new Button {
      text = "Feeling Lucky"
    }
    contents = new BorderPanel {

      import scala.swing.BorderPanel.Position._

      layout(new BorderPanel {
        layout(new Label("URL:")) = West
        layout(urlField) = Center
        layout(button) = East
      }) = North
      layout(pageField) = Center
    }
    size = new Dimension(1024, 768)
  }

  implicit class ButtonOpts(val self: Button) {
    def clicks: Observable[Unit] = Observable { o =>
      self.reactions += {
        case ButtonClicked(_) => o.onNext(())
      }
      Subscription()
    }
  }

  implicit class TextFieldOpts(val self: TextField) {
    def texts: Observable[String] = Observable { o =>
      self.reactions += {
        case ValueChanged(_) => o.onNext(self.text)
      }
      Subscription()
    }
  }

  trait BrowserLogic {
    self: BrowserFrame =>
    def suggestRequest(term: String): Observable[String] = {
      val url = s"http://suggestqueries.google.com/complete/search?client=firefox&q=$term"
      val request = Future {
        Source.fromURL(url).mkString
      }
      Observable.from(request)
        .timeout(0.5.seconds)
        .onErrorReturn(_ => "no suggestion")
    }

    def pageRequest(url: String): Observable[String] = {
      val request = Future {
        Source.fromURL(url).mkString
      }
      Observable.from(request)
        .timeout(5.seconds)
        .onErrorReturn(e => s"Could not load page: $e")
    }

    urlField.texts.map(suggestRequest).concat
      .observeOn(swingScheduler)
      .subscribe(response => pageField.text = response)
    button.clicks.map(_ => pageRequest(urlField.text)).concat
      .observeOn(swingScheduler)
      .subscribe(response => pageField.text = response)
  }

  def top: Frame = new BrowserFrame with BrowserLogic
}
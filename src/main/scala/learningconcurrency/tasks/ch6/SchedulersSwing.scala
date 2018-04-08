package learningconcurrency.tasks.ch6

import learningconcurrency.tasks._
import rx.lang.scala.{Observable, Subscription}

import scala.swing.event.ButtonClicked
import scala.swing.{Button, Frame, MainFrame, SimpleSwingApplication}

object SchedulersSwing extends SimpleSwingApplication {
  def top: Frame = new MainFrame {
    title = "Swing Observables"
    val button = new Button {
      text = "Click"
    }
    contents = button
    val buttonClicks = Observable[Button] { o =>
      button.reactions += {
        case ButtonClicked(_) => o.onNext(button)
      }
      Subscription()
    }
    buttonClicks.subscribe(_ => log("button clicked"))
  }
}

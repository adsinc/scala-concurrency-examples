package learningconcurrency.tasks.ch6

import java.awt.Point
import java.awt.RenderingHints.{KEY_ANTIALIASING, VALUE_ANTIALIAS_ON}

import javax.swing.SwingUtilities
import rx.lang.scala.{Observable, Scheduler, Subscription}
import rx.schedulers.Schedulers

import scala.collection.mutable
import scala.swing.event._
import scala.swing.{Component, Dimension, Frame, Graphics2D, MainFrame, SimpleSwingApplication}


object Paint extends SimpleSwingApplication {

  val swingScheduler = new Scheduler {
    val asJavaScheduler: rx.Scheduler = Schedulers.from((command: Runnable) => SwingUtilities.invokeLater(command))
  }

  class RxCanvas extends Component {

    listenTo(mouse.clicks)
    listenTo(mouse.moves)

    private def observe(eventToPoint: PartialFunction[Event, Point]) =
      Observable[(Int, Int)] { o =>
        reactions += eventToPoint.andThen(p => o.onNext(p.x -> p.y))
      }.observeOn(swingScheduler)

    def mouseMoves: Observable[(Int, Int)] = observe {
      case MouseMoved(_, p, _) => p
      case MouseDragged(_, p, _) => p
    }

    def mousePresses: Observable[(Int, Int)] = observe {
      case MousePressed(_, p, _, _, _) => p
    }

    def mouseReleases: Observable[(Int, Int)] = observe {
      case MouseReleased(_, p, _, _, _) => p
    }
  }

  trait PaintLogic {
    self: RxCanvas =>
    type Point = (Int, Int)
    type Line = (Point, Point)
    private val lines = mutable.Set.empty[Line]

    override protected def paintComponent(g: Graphics2D): Unit = {
      g.setRenderingHint(KEY_ANTIALIASING, VALUE_ANTIALIAS_ON)
      lines.foreach {
        case ((x1, y1), (x2, y2)) => g.drawLine(x1, y1, x2, y2)
      }
    }

    var moves: Option[Subscription] = None

    private def startPaint(): Unit = {
      moves = Some {
        mouseMoves zip mouseMoves.tail subscribe { line =>
          lines += line
          repaint()
        }
      }
    }

    private def stopPaint(): Unit = {
      moves.foreach(_.unsubscribe())
      moves = None
    }

    mousePresses.subscribe(_ => startPaint())
    mouseReleases.subscribe(_ => stopPaint())

  }

  def top: Frame = new MainFrame {
    title = "Rx Paint"
    contents = new RxCanvas with PaintLogic
    size = new Dimension(800, 600)
  }
}

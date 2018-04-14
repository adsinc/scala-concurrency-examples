package learningconcurrency.tasks.ch6

import learningconcurrency.tasks._
import rx.lang.scala.{Observable, Subscription}

import scala.concurrent.duration.DurationInt

class Signal[T] {
  protected var observable: Observable[T] = _
  protected var lastValue: Option[T] = None
  private var subscription: Option[Subscription] = None

  def this(observable: Observable[T], lastValue: Option[T] = None) {
    this()
    this.lastValue = lastValue
    setObservable(observable)
  }

  protected def setObservable(observable: Observable[T]): Unit = {
    this.observable = observable
    subscription.foreach(_.unsubscribe())
    subscription = Some {
      observable.subscribe { t => lastValue = Some(t) }
    }
  }

  def apply(): T = lastValue.get

  def map[S](f: T => S): Signal[S] =
    new Signal(observable map f, lastValue map f)

  def zip[S](that: Signal[S]): Signal[(T, S)] =
    new Signal(
      observable zip that.observable,
      for {
        v1 <- lastValue
        v2 <- that.lastValue
      } yield (v1, v2)
    )

  def scan[S](z: S)(f: (S, T) => S): Signal[S] =
    new Signal(observable.scan(z)(f))
}

object Signal {

  implicit class ObservableOpts[T](self: Observable[T]) {
    def toSignal: Signal[T] = new Signal(self)
  }

}

object SignalTest extends App {

  import Signal._

  val obs = Observable.interval(200.millis).take(10)
  obs.subscribe(e => log(s"Event from observable 1: $e"))

  val obs2 = Observable.interval(500.millis).take(5)
  obs2.subscribe(e => log(s"Event from observable 2: $e"))

  val s = obs.toSignal zip obs2.toSignal

  Thread.sleep(1000)
  log(s"Signal value: ${s()}")
  Thread.sleep(2000)
  log(s"Signal value: ${s()}")
  Thread.sleep(3000)
  log(s"Signal value: ${s()}")

  Thread.sleep(2000)
}

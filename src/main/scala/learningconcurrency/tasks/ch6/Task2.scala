package learningconcurrency.tasks.ch6

import learningconcurrency.tasks._
import rx.lang.scala.Observable

import scala.concurrent.duration.DurationLong

object Task2 extends App {

  def createInterval(intervalSec: Long) =
    Observable.interval(intervalSec.seconds)
      .map(_ + 1)
      .map(_ * intervalSec)

  val s5 = createInterval(5)
  val s12 = createInterval(12)

  val ticker = Observable.from(Seq(s5, s12))
    .flatten
    .distinct
    .filterNot(_ % 30 == 0)

  ticker.subscribe(t => log(s"Tick $t"))

  Thread.sleep(2.minutes.toMillis)
}

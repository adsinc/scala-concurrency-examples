package learningconcurrency.tasks
package ch6

import rx.lang.scala._

import scala.concurrent.duration._

object ObservableTest extends App {
  val o = Observable.timer(1.second)
  o.subscribe(s => log(s"Timeout!"))
  o.subscribe(s => log(s"Another timeout!"))
  Thread.sleep(2000)
}

package learningconcurrency.tasks.ch6

import learningconcurrency.tasks._
import rx.lang.scala.Subject

class RCell[T] extends Signal[T] {
  private val subject = Subject[T]()
  setObservable(subject)

  def :=(t: T): Unit = {
    subject.onNext(t)
  }
}

object RCellTest extends App {

  val c = new RCell[Int]

  for {
    i <- 1 to 10
  } {
    c := i
    log(c())
  }
}
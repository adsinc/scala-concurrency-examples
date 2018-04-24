package learningconcurrency.tasks.ch6

import learningconcurrency.tasks._
import rx.lang.scala.{Observable, Subject}

import scala.annotation.tailrec
import scala.concurrent.Future

object ScatterGather extends App {

  implicit class ObservableOpts[T](self: Observable[T]) {
    def scatterGather[S](f: T => S): Observable[S] = {
      import scala.concurrent.ExecutionContext.Implicits.global
      val subject = Subject[S]()
      self subscribe { t =>
        Observable.from(Future(f(t))) subscribe subject.onNext _
      }
      subject
    }
  }

  @volatile var nv = 0

  @tailrec
  def countDown(n: Int): Unit = {
    nv = n
    if (n > 0) countDown(n - 1)
  }

  def f[T](e: T): Unit = {
    countDown(10000)
  }

  def observable = Observable.from(Seq.range(1, 1000))

  warmedTime("Sequence") {
    observable map f subscribe (_ => ())
  }

  warmedTime("Par") {
    observable scatterGather f subscribe (_ => ())
  }

}

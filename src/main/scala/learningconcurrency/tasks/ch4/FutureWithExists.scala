package learningconcurrency.tasks
package ch4

import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

import scala.concurrent.ExecutionContext.Implicits.global

object FutureWithExists extends App{

  implicit class FutureOpts[T](self: Future[T]) {
    def exists(p: T => Boolean): Future[Boolean] = self map p
  }

  val f1 = Future(1)
  val f2 = Future(-1)
  val f3 = Future[Int] {
    throw new Exception("Error!")
  }

  log(s"f1=${Await.result(f1.exists(_ > 0), Duration.Inf)}")
  log(s"f2=${Await.result(f2.exists(_ > 0), Duration.Inf)}")

//  log(s"f3=${Await.result(f3.exists(_ > 0), Duration.Inf)}")
  f3.failed foreach {
    t => log(t.getMessage)
  }

}

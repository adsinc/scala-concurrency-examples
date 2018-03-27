package learningconcurrency.tasks
package ch4

import scala.async.Async._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration.Duration
import scala.concurrent.{Await, Future}

object FutureExistsViaAsync extends App {

  implicit class FutureOpts[T](self: Future[T]) {
    def exists(p: T => Boolean): Future[Boolean] =
      async {
        val t = await(self)
        p(t)
      }

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

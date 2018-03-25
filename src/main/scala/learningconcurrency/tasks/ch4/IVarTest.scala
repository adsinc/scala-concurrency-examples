package learningconcurrency.tasks
package ch4

import scala.concurrent.{Await, Promise}
import scala.concurrent.duration._
import scala.concurrent._
import scala.util.{Random, Try}

object IVarTest extends App {

  class IVar[T] {
    private val p = Promise[T]
    private val f = p.future

    def apply(): T = Await.result(f, Duration.Zero)

    def :=(x: T): Unit = p success x
  }

  val iVar = new IVar[Int]

  for(i <- 1 to 50) yield execute {
    blocking {
      Thread.sleep(Random.nextInt(20))
      println(s"Get value result ${Try(iVar())}")
    }
  }

  for(i <- 1 to 10) yield execute {
    blocking {
      Thread.sleep(Random.nextInt(20))
      println(s"Set value $i with result ${Try(iVar := i)}")
    }
  }

  Thread.sleep(1000)
}

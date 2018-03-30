package learningconcurrency.tasks
package ch4

import java.util.concurrent.ConcurrentHashMap

import scala.collection.JavaConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.duration._
import scala.concurrent.{Await, Future, Promise}
import scala.util.Random

class IMap[K, V] {
  private val m = new ConcurrentHashMap[K, Promise[V]]().asScala

  def update(k: K, v: V): Unit =
    m.getOrElseUpdate(k, Promise[V]).success(v)


  def apply(k: K): Future[V] =
    m.getOrElseUpdate(k, Promise[V]).future
}

object IMapTest extends App {

  val m = new IMap[Int, Int]

  val vals = for {
    i <- 1 to 10
  } yield m(i)

  for {
    i <- 1 to 10
    j = Random.nextInt()
  } execute(m(i) = j)

  Await.result(Future.sequence(vals).map(println), 2.second)

}
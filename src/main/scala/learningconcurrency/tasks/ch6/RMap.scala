package learningconcurrency.tasks.ch6

import learningconcurrency.tasks._
import rx.lang.scala.{Observable, Subject}

import scala.collection.mutable

class RMap[K, V] {
  private val subjects = mutable.Map[K, Subject[V]]()
  private val m = mutable.Map[K, Seq[V]]()

  def update(k: K, v: V): Unit = {
    m(k) = m.getOrElseUpdate(k, Vector.empty) :+ v
    subjects
      .getOrElseUpdate(k, Subject())
      .onNext(v)
  }

  def apply(k: K): Observable[V] = {
    val previous = Observable.from[V](m.getOrElse(k, Vector.empty))
    val next = Observable[V] { subscriber =>
      subjects.getOrElseUpdate(k, Subject()).subscribe { v =>
        subscriber.onNext(v)
      }
    }
    previous merge next
  }
}

object RMapTest extends App {
  val rMap = new RMap[Int, Int]

  rMap(1) subscribe (i => log(s"First subscriber: $i"))

  for {
    i <- 1 to 3
  } rMap(1) = i

  rMap(1) subscribe (i => log(s"Second subscriber: $i"))

  for {
    i <- 4 to 6
  } rMap(1) = i

  val s = Subject()


  Thread.sleep(1000)
}
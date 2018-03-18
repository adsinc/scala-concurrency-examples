package learningconcurrency.tasks
package ch3

import java.util.concurrent.atomic.AtomicReference

import scala.annotation.tailrec

class LazyCell[T](init: => T) {
  private var value: Option[T] = None

  def apply(): T = value.synchronized {
    if (value.isEmpty) {
      value = Some(init)
    }
    value.get
  }
}

object LazyCellTest extends App {
  val lc = new LazyCell[Int]({
    println("Init lazy cell")
    1
  })
  println("Lazy cell created")

  (for (i <- 1 to 8) yield thread {
    println(s"Get lazy cell value: ${lc()}")
  }).foreach(_.join)
}

class PureLazyCell[T](init: => T) {
  private val r = new AtomicReference[Option[T]](None)

  @tailrec
  final def apply(): T = {
    val value = r.get()
    value match {
      case Some(v) => v
      case None =>
        val value = init
        if (!r.compareAndSet(None, Some(value))) apply()
        else value
    }
  }
}

object PureLazyCellTest extends App {
  val lc = new PureLazyCell[Long](System.nanoTime())
  println("Lazy cell created")

  (for (i <- 1 to 10) yield thread {
    println(s"Get lazy cell value: ${lc()}")
  }).foreach(_.join)
}
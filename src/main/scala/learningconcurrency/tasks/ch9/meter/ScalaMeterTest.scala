package learningconcurrency.tasks.ch9.meter

import java.util.concurrent.atomic.AtomicReference

import org.scalameter._

import scala.annotation.tailrec

object ScalaMeterTest extends App {

  class Accumulator[T](z: T)(op: (T, T) => T) {
    private val value = new AtomicReference(z)

    def apply(): T = value.get()

    @tailrec final def add(v: T): Unit = {
      val ov = value.get()
      val nv = op(ov, v)
      if (!value.compareAndSet(ov, nv)) add(v)
    }
  }

  val accTime = config(
    Key.exec.minWarmupRuns -> 20,
    Key.exec.maxWarmupRuns -> 100,
    Key.exec.benchRuns -> 1000,
    Key.verbose -> true
  ) withWarmer new Warmer.Default measure {
    val acc = new Accumulator(0)(_ + _)
    var i = 0
    val total = 1000000
    while (i < total) {
      acc.add(i)
      i += 1
    }
  }

  println(s"Accumulator time: " + accTime)

}

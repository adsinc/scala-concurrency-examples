package learningconcurrency.tasks
package ch9.meter

import java.util.concurrent.atomic.{AtomicLong, AtomicLongArray, AtomicReference}

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
    Key.exec.maxWarmupRuns -> 50,
    Key.exec.benchRuns -> 30,
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
  println("*" * 80)

  class AccumulatorLong(z: Long)(op: (Long, Long) => Long) {
    private val value = new AtomicLong(z)

    def apply(): Long = value.get()

    @tailrec final def add(v: Long): Unit = {
      val ov = value.get()
      val nv = op(ov, v)
      if (!value.compareAndSet(ov, nv)) add(v)
    }
  }

  val accTimeLong = config(
    Key.exec.minWarmupRuns -> 20,
    Key.exec.maxWarmupRuns -> 50,
    Key.exec.benchRuns -> 30,
    Key.verbose -> true
  ) withWarmer new Warmer.Default measure {
    val acc = new AccumulatorLong(0)(_ + _)
    var i = 0
    val total = 1000000
    while (i < total) {
      acc.add(i)
      i += 1
    }
  }

  println(s"Accumulator time: $accTimeLong")
  println("*" * 80)

  val longAccTime4 = config(
    Key.exec.minWarmupRuns -> 20,
    Key.exec.maxWarmupRuns -> 40,
    Key.exec.benchRuns -> 30,
    Key.verbose -> true
  ) withWarmer new Warmer.Default measure {
    val acc = new AccumulatorLong(0)(_ + _)
    val total = 1000000
    val p = 4
    val threads = for {
      j <- 0 until p
    } yield thread {
      val start = j * total / p
      var i = start
      while (i < start + total / p) {
        acc.add(i)
        i += 1
      }
    }
    threads foreach (_.join)
  }

  println(s"4 threads long accumulator time: $longAccTime4")
  println("*" * 80)

  val longAccTime8 = config(
    Key.exec.minWarmupRuns -> 20,
    Key.exec.maxWarmupRuns -> 40,
    Key.exec.benchRuns -> 30,
    Key.verbose -> true
  ) withWarmer new Warmer.Default measure {
    val acc = new AccumulatorLong(0)(_ + _)
    val total = 1000000
    val p = 8
    val threads = for {
      j <- 0 until p
    } yield thread {
      val start = j * total / p
      var i = start
      while (i < start + total / p) {
        acc.add(i)
        i += 1
      }
    }
    threads foreach (_.join)
  }

  println(s"8 threads long accumulator time: $longAccTime8")
  println("*" * 80)

  import scala.util.hashing

  class ParAccumulatorLong(z: Long)(op: (Long, Long) => Long) {
    private val par = Runtime.getRuntime.availableProcessors() * 128
    private val values = new AtomicLongArray(par)

    def apply(): Long = {
      var total = z
      for (i <- 0 until values.length()) {
        total = op(total, values.get(i))
      }
      total
    }

    @tailrec final def add(v: Long): Unit = {
      val id = Thread.currentThread().getId.toInt
      val pos = math.abs(hashing.byteswap32(id)) % par
      val ov = values.get(pos)
      val nv = op(ov, v)
      if (!values.compareAndSet(pos, ov, nv)) add(v)
    }
  }

  val longParAccTime4 = config(
    Key.exec.minWarmupRuns -> 20,
    Key.exec.maxWarmupRuns -> 50,
    Key.exec.benchRuns -> 30,
    Key.verbose -> true
  ) withWarmer new Warmer.Default measure {
    val acc = new ParAccumulatorLong(0)(_ + _)
    val total = 1000000
    val p = 4
    val threads = for {
      j <- 0 until p
    } yield thread {
      val start = j * total / p
      var i = start
      while (i < start + total / p) {
        acc.add(i)
        i += 1
      }
    }
    threads foreach (_.join)
  }

  println(s"4 threads long parallel accumulator time: $longParAccTime4")
  println("*" * 80)

  val longParAccTime8 = config(
    Key.exec.minWarmupRuns -> 20,
    Key.exec.maxWarmupRuns -> 50,
    Key.exec.benchRuns -> 30,
    Key.verbose -> true
  ) withWarmer new Warmer.Default measure {
    val acc = new ParAccumulatorLong(0)(_ + _)
    val total = 1000000
    val p = 8
    val threads = for {
      j <- 0 until p
    } yield thread {
      val start = j * total / p
      var i = start
      while (i < start + total / p) {
        acc.add(i)
        i += 1
      }
    }
    threads foreach (_.join)
  }

  println(s"8 threads long parallel accumulator time: $longParAccTime8")
  println("*" * 80)

}

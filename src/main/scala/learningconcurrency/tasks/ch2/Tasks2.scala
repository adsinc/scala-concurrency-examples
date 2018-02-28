package learningconcurrency.tasks
package ch2

import learningconcurrency.tasks.ch2.Tasks2._

object Tasks2 {

  def thread(block: => Unit): Thread = {
    val t = new Thread() {
      override def run(): Unit = block
    }
    t.start()
    t
  }

  def parallel[A, B](a: => A, b: => B): (A, B) = {
    var resA: A = null.asInstanceOf[A]
    var resB: B = null.asInstanceOf[B]

    val ta = thread {
      resA = a
      log(s"Calculated value for a: $resA")
    }

    val tb = thread {
      resB = b
      log(s"Calculated value for b: $resB")
    }

    ta.join()
    tb.join()

    (resA, resB)
  }

  def periodically(duration: Long)(block: => Unit): Unit =
    thread {
      while (true) {
        block
        Thread.sleep(duration)
      }
    }

}

object TestParallel extends App {
  val sqrts = (1 to 5) zip (11 to 15) map { p =>
    parallel(math.sqrt(p._1), math.sqrt(p._2))
  }

  println(sqrts)
}

object TestPeriodically extends App {
  periodically(1000)(println("Kill all human"))
}
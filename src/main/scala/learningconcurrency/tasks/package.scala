package learningconcurrency

import scala.concurrent.ExecutionContext

package object tasks {

  def log(msg: Any) {
    println(s"${Thread.currentThread.getName}: $msg")
  }

  def time[T](name: String)(body: => T): T = {
    val start = System.nanoTime()
    val r = body
    val time = (System.nanoTime() - start) / 1000 / 1000.0
    log(s"$name executed at $time ms, result: $r")
    r
  }

  def warmedTime[T](name: String, n: Int = 200)(body: => T): T = {
    for (_ <- 0 until n) body
    time(name)(body)
  }

  def warm[T](body: => T, n: Int = 200): Unit = {
    for (_ <- 0 until n) body
  }

  @volatile private var dummy: Any = _

  def timed[T](body: => T): Double = {
    val start = System.nanoTime()
    dummy = body
    ((System.nanoTime() - start) / 1000) / 1000.0
  }

  def thread(block: => Unit): Thread = {
    val t = new Thread() {
      override def run(): Unit = block
    }
    t.start()
    t
  }

  def execute(body: => Unit): Unit = ExecutionContext.global.execute {
    () => body
  }
}

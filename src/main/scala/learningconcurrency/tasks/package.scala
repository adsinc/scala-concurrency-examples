package learningconcurrency

import scala.concurrent.ExecutionContext

package object tasks {

  def log(msg: String) {
    println(s"${Thread.currentThread.getName}: $msg")
  }

  def time[T](name: String)(body: => T): T = {
    val start = System.nanoTime()
    val r = body
    val time = (System.nanoTime() - start) / 1000 / 1000.0
    log(s"$name executed at $time")
    r
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

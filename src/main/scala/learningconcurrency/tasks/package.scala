package learningconcurrency

import scala.concurrent.ExecutionContext

package object tasks {

  def log(msg: String) {
    println(s"${Thread.currentThread.getName}: $msg")
  }

  def time[T](name: String)(body: => T): T = {
    val start = System.currentTimeMillis()
    val r = body
    log(s"$name executed at ${System.currentTimeMillis() - start}")
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

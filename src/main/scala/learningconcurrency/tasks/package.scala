package learningconcurrency

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

}

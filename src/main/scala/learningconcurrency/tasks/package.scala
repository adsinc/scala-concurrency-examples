package learningconcurrency

package object tasks {

  def log(msg: String) {
    println(s"${Thread.currentThread.getName}: $msg")
  }

}

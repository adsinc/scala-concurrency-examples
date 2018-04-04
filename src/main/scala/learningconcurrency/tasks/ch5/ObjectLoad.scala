package learningconcurrency.tasks
package ch5

object ObjectLoad extends App {

  val steps = 10000000

  @volatile var dummy: Any = _

  val times = for(i <- 0 to steps) yield timed {
    dummy = new Object
  }

  log(s"Object create time: ${times.sum / steps} ms")

}

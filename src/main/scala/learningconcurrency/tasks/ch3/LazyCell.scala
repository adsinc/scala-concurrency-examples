package learningconcurrency.tasks.ch3

class LazyCell[T](init: => T) {
  def apply(): T = init
}

object LazyCellTest extends App {
  val lc = new LazyCell[Int]({
    println("Init lazy cell")
    1
  })
  println("Lazy cell created")
  println(s"Get lazy cell value: ${lc()}")
}

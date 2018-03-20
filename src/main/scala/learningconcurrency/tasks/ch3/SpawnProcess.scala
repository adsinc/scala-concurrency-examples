package learningconcurrency.tasks.ch3

import scala.sys.process._

object SpawnProcess extends App {
  def spawn[T](block: => T): T = ???
}

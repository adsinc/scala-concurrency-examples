package learningconcurrency.tasks
package ch5

import scala.util.Random

object ParMaxTest extends App {
  val numbers = Random.shuffle(Vector.tabulate(5000000)(identity))

  time("Sequence max") {
    numbers.max
  }

  time("Par max") {
    numbers.par.max
  }
}

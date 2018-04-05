package learningconcurrency.tasks
package ch5

import java.util.concurrent.atomic.AtomicInteger

import scala.collection.GenIterable

object RandomStringSpaceCount extends App {

  def genChar(spaceChance: Double): Char = {
    if (math.random() <= spaceChance)
      ' '
    else
      'q'
  }

  def count(s: GenIterable[Char]) = {
    val cnt = new AtomicInteger()
    s.foreach { c =>
      if (c == ' ') cnt.incrementAndGet()
    }
    cnt
  }

  def genString(p: Double, n: Int = 100000) =
    Seq.tabulate(n)(_ => genChar(p))

  val s = genString(0.5)
  warm {
    count(s)
  }

  for (p <- 0.0.to(1.0, 0.05)) {

    val str = genString(p, 10000000)
    time(s"p=$p Sequence space count")(count(str))

    val parStr = str.par
    time(s"p=$p Parallel space count")(count(parStr))
  }
}

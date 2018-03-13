package learningconcurrency.tasks
package ch3

import java.util.concurrent.atomic.AtomicReference

import scala.annotation.tailrec
import scala.util.Random

class TreiberStack[T](emptyValue: T) {

  private val data = new AtomicReference[List[T]]()
  data.set(List.empty[T])

  def push(x: T): Unit = {
    @tailrec def loop(x: T): Unit = {
      val xs = data.get()
      if (!data.compareAndSet(xs, x :: xs))
        loop(x)
    }

    loop(x)
  }

  def pop(): T = {
    @tailrec def loop(): T = {
      val ds = data.get()
      ds match {
        case Nil => emptyValue
        case x :: xs =>
          if (!data.compareAndSet(ds, xs)) loop()
          else x
      }
    }

    loop()
  }
}

object TreiberStackTest extends App {
  val stack = new TreiberStack[Int](-1)

  val t1 = thread {
    @tailrec def retryIfEmpty(n: Int): Int =
      if (n == -1) retryIfEmpty(stack.pop())
      else n

    for (_ <- 1 to 1000) {
      println(retryIfEmpty(stack.pop()))
      Thread.sleep(Random.nextInt(10))
    }
  }

  val t2 = thread {
    for (i <- 1 to 1000) {
      stack.push(i)
      Thread.sleep(Random.nextInt(10))
    }
  }

  t1.join()
  t2.join()
}
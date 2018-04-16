package learningconcurrency.tasks.ch6

import learningconcurrency.tasks._
import rx.lang.scala.{Observable, Subject}

import scala.util.Random

class RPriorityQueue[T]()(implicit ordering: Ordering[T]) {
  private val subject = Subject[T]()
  private var data = List.empty[T]

  def add(x: T): Unit = {
    import ordering._
    data = data.takeWhile(_ < x) ::: x :: data.dropWhile(_ < x)
  }

  def pop(): T = {
    val r = data.head
    data = data.tail
    subject.onNext(r)
    r
  }

  def popped: Observable[T] =
    Observable { obs =>
      subject.subscribe(obs.onNext _)
    }
}

object RPriorityQueueTest extends App {
  val q = new RPriorityQueue[Int]

  q.popped subscribe (t => log(s"$t was popped"))

  for {
    _ <- 1 to 10
  } q.add(Random.nextInt(100))

  for {
    _ <- 1 to 10
  } q.pop()

}
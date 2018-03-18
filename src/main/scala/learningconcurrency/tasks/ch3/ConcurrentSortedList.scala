package learningconcurrency.tasks
package ch3

import java.util.concurrent.atomic.AtomicReference

import scala.annotation.tailrec
import scala.util.Random

class ConcurrentSortedList[T](implicit val ord: Ordering[T]) {

  case class Node(head: T,
                  tail: AtomicReference[Option[Node]] = new AtomicReference[Option[Node]](None))

  val root = new AtomicReference[Option[Node]](None)

  def add(t: T): Unit = {
    add(root, t)
  }

  @tailrec
  private def add(r: AtomicReference[Option[Node]], t: T): Unit = {
    val optNode = r.get()
    optNode match {
      case None =>
        if (!r.compareAndSet(optNode, Some(Node(t))))
          add(r, t)
      case Some(node) =>
        if (ord.lteq(t, node.head)) {
          val newNode = Node(t)
          newNode.tail.set(optNode)
          if (!r.compareAndSet(optNode, Some(newNode)))
            add(r, t)
        } else {
          add(node.tail, t)
        }
    }
  }

  def iterator: Iterator[T] = new Iterator[T] {
    private var data = root.get()

    def hasNext: Boolean = data.isDefined

    def next(): T = data match {
      case None => throw new NoSuchElementException("Iterator is empty")
      case Some(node) =>
        data = node.tail.get()
        node.head
    }
  }
}

object ConcurrentSortedListTest extends App {

  val l = new ConcurrentSortedList[Int]()

  (for (_ <- 1 to 8) yield thread {
    for (_ <- 1 to 10) {
      Thread.sleep(Random.nextInt(30))
      l.add(Random.nextInt(10000))
    }
  }) foreach (_.join())

  l.iterator.foreach(println)
}
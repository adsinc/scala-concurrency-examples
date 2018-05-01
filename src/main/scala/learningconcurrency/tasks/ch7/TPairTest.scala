package learningconcurrency.tasks.ch7

import learningconcurrency.tasks._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.stm._

object TPairTest extends App {

  class TPair[P, Q](pInit: P, qInit: Q) {
    private val p = Ref(pInit)
    private val q = Ref(qInit)

    def first(implicit txn: InTxn): P = p()

    def first_=(x: P)(implicit txn: InTxn): Unit = p.single() = x

    def second(implicit txn: InTxn): Q = q()

    def second_=(y: Q)(implicit txn: InTxn): Unit = q.single() = y

    def swap()(implicit e: P =:= Q, e2: Q =:= P,txn: InTxn): Unit = {
      val tmp = q()
      q() = p()
      p() = tmp
    }
  }

  val p = new TPair(0, "empty")

  def printPair[P, Q](p: TPair[P, Q]): Unit = atomic { implicit tx =>
    log(s"First=${p.first}")
    log(s"Second=${p.second}")
  }

  Future {
    atomic { implicit tx =>
      p.first = 1
      p.second = "Hello"
    }
  }

  Future {
    atomic { implicit tx =>
      p.first = 2
      p.second = "World"
    }
  }
  Thread.sleep(100)
  printPair(p)

  val p1 = new TPair(1, 2)
  atomic { implicit tx =>
    p1.swap()
  }
  printPair(p1)
}

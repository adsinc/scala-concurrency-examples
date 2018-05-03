package learningconcurrency.tasks.ch7

import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicInteger

import learningconcurrency.tasks._

import scala.collection.immutable.Queue
import scala.concurrent.stm._
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.Random

object TQueueTest extends App {

  val pool = Executors.newCachedThreadPool()
  implicit val executionContext: ExecutionContextExecutor = ExecutionContext.fromExecutor(pool)

  class TQueue[T] {
    private val xs = Ref(Queue.empty[T])

    def enqueue(x: T)(implicit txn: InTxn): Unit = {
      xs.single.transform(_ enqueue x)
    }

    def dequeue()(implicit txn: InTxn): T = {
      if (xs().isEmpty) {
        retry
      } else {
        val t = xs().head
        xs() = xs().tail
        t
      }
    }
  }

  val q = new TQueue[Int]
  val n = new AtomicInteger()
  val local = TxnLocal[Int]()

  def addPack(): Unit = for {
    _ <- 1 to 3
    c = n.incrementAndGet()
  } Future {
    atomic { implicit txn =>
      q.enqueue(c)
    }
  }

  Future {
    while (true) {
      val v = atomic { implicit txn =>
        q.dequeue()
      }
      log(s"Received value: $v")
    }
  }

  for {
    _ <- 1 to 5
  } {
    Thread.sleep(Random.nextInt(1000) + 500)
    addPack()
  }

  Thread.sleep(5000)
  pool.shutdown()
}

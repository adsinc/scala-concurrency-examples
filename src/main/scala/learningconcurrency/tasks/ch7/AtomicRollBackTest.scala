package learningconcurrency.tasks.ch7

import learningconcurrency.tasks.log

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.stm._
import scala.util.Failure

object AtomicRollBackTest extends App {

  def atomicRollbackCount[T](block: InTxn => T): (T, Int) = {
    var rollbackCount = 0
    atomic { implicit txn =>
      Txn.afterRollback(_ => rollbackCount += 1)
      (block(txn), rollbackCount)
    }
  }

  val count = Ref[Int](0)

  def inc(txn: InTxn): Int = {
    implicit val t: InTxn = txn
    count += 1
    count()
  }

  1 to 100 foreach (_ => Future {
    log(atomicRollbackCount(inc))
  })

  Thread.sleep(1000)

  def atomicWithRetryMax[T](n: Int)(block: InTxn => T): T = {
    var retryCount = 0
    atomic { implicit txn =>
      if (retryCount < n) {
        Txn.afterRollback(_ => retryCount += 1)
        block(txn)
      } else {
        throw new RuntimeException("Retry limit reached")
      }
    }
  }

  1 to 100 foreach (_ => Future {
    log(atomicWithRetryMax(3)(inc))
  } onComplete {
    case Failure(e) => log(e.getMessage)
    case _ =>
  })

  Thread.sleep(1000)


}

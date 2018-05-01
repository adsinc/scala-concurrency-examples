package learningconcurrency.tasks.ch7

import java.util.concurrent.atomic.AtomicLong

import learningconcurrency.tasks._

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.stm._

object MVarTest extends App {

  class MVar[T] {
    private val ref = Ref[Option[T]](None)

    def put(x: T)(implicit txn: InTxn): Unit = ref() match {
      case Some(_) => retry
      case None => ref() = Some(x)
    }

    def take()(implicit txn: InTxn): T = ref() match {
      case Some(_) => (ref swap None).get
      case None => retry
    }
  }

  def swap[T](m1: MVar[T], m2: MVar[T]): Unit = atomic { implicit txn =>
    val v1 = m1.take()
    m1 put m2.take()
    m2 put v1
  }

  val mVar = new MVar[Int]
  val sum = new AtomicLong()
  val ns = 1 to 100

  ns foreach { i => Future { atomic { implicit txn =>
    mVar put i
  }}}

  ns foreach { i => Future { atomic { implicit txn =>
    val v = mVar.take()
    Txn.afterCommit(_ => sum.addAndGet(v))
  }}}

  Thread.sleep(2000)

  log(ns.sum)
  log(sum.get())

  // test swap

  val v1 = new MVar[Int]
  val v2 = new MVar[Int]

  atomic { implicit txn =>
    v1 put 42
    v2 put 99
  }

  1 to 1001 foreach { _ =>
    swap(v1, v2)
  }

  atomic { implicit txn =>
    log(s"v1=${v1.take()}")
    log(s"v2=${v2.take()}")
  }
}

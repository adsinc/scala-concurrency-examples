package learningconcurrency.tasks
package ch2

import learningconcurrency.tasks.ch2.Tasks2.SynchronizedProtectedUid.getUniqueId
import learningconcurrency.tasks.ch2.Tasks2._

import scala.collection.immutable.IndexedSeq

object Tasks2 {

  def thread(block: => Unit): Thread = {
    val t = new Thread() {
      override def run(): Unit = block
    }
    t.start()
    t
  }

  def parallel[A, B](a: => A, b: => B): (A, B) = {
    var resA: A = null.asInstanceOf[A]
    var resB: B = null.asInstanceOf[B]

    val ta = thread {
      resA = a
      log(s"Calculated value for a: $resA")
    }

    val tb = thread {
      resB = b
      log(s"Calculated value for b: $resB")
    }

    ta.join()
    tb.join()

    (resA, resB)
  }

  def periodically(duration: Long)(block: => Unit): Unit =
    thread {
      while (true) {
        block
        Thread.sleep(duration)
      }
    }

  class SyncVar[T] {
    var v: T = null.asInstanceOf[T]
    var empty = true

    def get(): T = this.synchronized {
      require(!empty, "No value in var")
      empty = true
      v
    }

    def put(x: T): Unit = this.synchronized {
      require(empty, "Var is not empty")
      v = x
      empty = false
    }

    def getWait: T = this.synchronized {
      if (empty)
        wait()
      empty = true
      this.notify()
      v
    }

    def putWait(x: T): Unit = this.synchronized {
      if (!empty)
        wait()
      v = x
      empty = false
      this.notify()
    }

    def isEmpty: Boolean = this.synchronized(empty)

    def nonEmpty: Boolean = this.synchronized(!empty)
  }

  class SyncQueue[T](size: Int) {
    private val q = collection.mutable.Queue[T]()

    def get(): T = this.synchronized {
      if (q.isEmpty)
        wait()
      val t = q.dequeue()
      notify()
      t
    }

    def put(x: T): Unit = this.synchronized {
      if (q.lengthCompare(size) >= 0)
        wait()
      q.enqueue(x)
      notify()
    }
  }

  object SynchronizedProtectedUid {
    var uidCount = 0L

    def getUniqueId: Long = this.synchronized {
      uidCount += 1
      uidCount
    }
  }

  class Account(var name: String, var money: Int) {
    val uid: Long = getUniqueId

    override def toString = s"Account($uid, $name, $money)"
  }

  def sendAll(accounts: Set[Account], target: Account): Unit = {
    val lockOrder = (accounts + target).toSeq sortBy (_.uid)

    def syncAll(acs: Seq[Account], action: => Unit): Unit =
      if (acs.isEmpty) action
      else acs.head.synchronized(syncAll(acs.tail, action))


    syncAll(lockOrder, {
      accounts.foreach { src =>
        if (src.uid != target.uid) {
          target.money += src.money
          src.money = 0
        }
      }
    })
  }
}

object TestParallel extends App {
  val sqrts = (1 to 5) zip (11 to 15) map { p =>
    parallel(math.sqrt(p._1), math.sqrt(p._2))
  }

  println(sqrts)
}

object TestPeriodically extends App {
  periodically(1000)(println("Kill all human"))
}

object TestSyncVarWithEmpty extends App {
  val syncVar = new SyncVar[Int]

  val s = thread {
    var i = 0
    while (i <= 15) {
      if (syncVar.isEmpty) {
        syncVar.put(i)
        i += 1
      }
    }
  }

  val c = thread {
    var x = 0
    while (x < 15) {
      if (syncVar.nonEmpty) {
        x = syncVar.get()
        println(x)
      }
    }
  }

  s.join()
  c.join()
}

object TestSyncVarWithWait extends App {
  val syncVar = new SyncVar[Int]

  val s = thread {
    0 to 15 foreach syncVar.putWait
  }

  val c = thread {
    var x = 0
    while (x < 15) {
      x = syncVar.getWait
      println(x)
    }
  }

  s.join()
  c.join()
}

object TestSyncQueueWithWait extends App {
  val n = 30
  val q = new SyncQueue[Int](10)

  val s = thread {
    0 to 30 foreach q.put
  }

  val c = thread {
    var x = 0
    while (x < 30) {
      x = q.get()
      println(x)
    }
  }

  s.join()
  c.join()
}

object TestSendAll extends App {
  val accounts: IndexedSeq[Account] = 1 to 10 map (i => new Account(s"Account $i", 10))

  val ts = for {
    i <- accounts.indices
  } yield thread {
    sendAll(accounts.toSet, accounts(i))
  }
  ts foreach (_.join())
  accounts foreach println
}

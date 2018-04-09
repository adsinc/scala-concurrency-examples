package learningconcurrency.tasks.ch6

import learningconcurrency.tasks._
import rx.lang.scala.Observable

import scala.annotation.tailrec
import scala.concurrent.duration.DurationLong

object ThreadRunMonitoring extends App {

  var currentThreads = Seq.empty[Thread]

  val root = getRootThread(Thread.currentThread().getThreadGroup)

  @tailrec
  def getRootThread(group: ThreadGroup): ThreadGroup = {
    if (group.getParent == null) group
    else getRootThread(group.getParent)
  }

  def getNewThreads: Seq[Thread] = {
    val threads = new Array[Thread](root.activeCount())
    root.enumerate(threads, true)
    val newT = threads.filterNot(currentThreads.contains)
    currentThreads = threads
    newT
  }

  def newThreadsObservable: Observable[Thread] =
    Observable { o =>
      getNewThreads foreach o.onNext
    }

  val newThreadObserver = for {
    _ <- Observable.interval(1.second)
    ts <- newThreadsObservable
  } yield ts

  newThreadObserver.subscribe(t => log(s"$t"))

  thread {
    Thread.sleep(5000)
  }
  Thread.sleep(1000)

  thread {
    Thread.sleep(5000)
  }
  Thread.sleep(2000)

  thread {
    Thread.sleep(5000)
  }
  Thread.sleep(3000)

  Thread.sleep(10000)
}

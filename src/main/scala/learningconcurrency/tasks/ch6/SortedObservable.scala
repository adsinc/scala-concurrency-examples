package learningconcurrency.tasks.ch6

import rx.lang.scala.{Observable, Subject}

import scala.collection.mutable.ArrayBuffer
import scala.concurrent.duration.DurationDouble

object SortedObservable extends App {

  implicit class ObservableOpts[T](self: Observable[T]) {
    def sorted(implicit ord: Ordering[T]): Observable[T] = {
      val s = Subject[T]()
      val c = s.cache
      val cache = ArrayBuffer.empty[T]
      self.subscribe(
        onNext = t => cache += t,
        onError = _ => (),
        onCompleted = () => {
          cache.sorted foreach s.onNext
        }
      )
      c
    }
  }

  Observable.from(List(1, 4, 2, 5, 3)).delay(100.millis)
    .sorted
    .subscribe(e => println(e))

  Thread.sleep(500)
}

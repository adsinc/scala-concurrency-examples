package learningconcurrency.tasks
package ch6

import rx.lang.scala._

import scala.io.Source

object RandomQuote extends App {

  def randomQuote = Observable[String] { obs =>
    val url = "http://quotesondesign.com/wp-json/posts?filter[orderby]=rand&filter[posts_per_page]=1"
    obs.onNext(Source.fromURL(url).getLines().mkString)
    obs.onCompleted()
    Subscription()
  }

  randomQuote.subscribe(log _)
}

package learningconcurrency.tasks
package ch6

import rx.lang.scala._

import scala.io.Source

object RandomQuote extends App {

  def randomQuote = Observable[String] { obs =>
    def extractQuote(q: String): String = {
      """<p>(.*)<""".r.findAllIn(q).group(1)
    }
    val url = "http://quotesondesign.com/wp-json/posts?filter[orderby]=rand&filter[posts_per_page]=1"
    obs.onNext(extractQuote(Source.fromURL(url).getLines().mkString))
    obs.onCompleted()
    Subscription()
  }

  randomQuote
    .map(_.length)
    .repeat(5)
    .scan((0, 0)) {
      case ((oldLen, cnt), l) => (oldLen + l, cnt + 1)
    }
    .tail
    .map { case (sum, cnt) => sum.toDouble / cnt }
    .subscribe(s => log(s.toString))
}

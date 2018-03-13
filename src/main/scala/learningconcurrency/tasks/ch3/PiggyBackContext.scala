package learningconcurrency.tasks
package ch3

import scala.concurrent.ExecutionContext
import scala.util.{Failure, Success, Try}

class PiggyBackContext extends ExecutionContext {
  def execute(runnable: Runnable): Unit = Try(runnable.run()) match {
    case Success(_) => log("OK!")
    case Failure(e) => reportFailure(e)
  }

  def reportFailure(cause: Throwable): Unit =
    log(s"Error: $cause")
}

object PiggyBackContextTest extends App {

  val ec = new PiggyBackContext

  ec.execute { () =>
    log(s"${1 / 2}")
  }

  thread {
    ec.execute { () =>
      log(s"${1 / 0}")
    }

    ec.execute { () =>
      ec.execute { () =>
        log("Inner call")
      }
    }
  }

}

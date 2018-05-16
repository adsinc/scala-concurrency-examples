package learningconcurrency.tasks.ch8

import scala.concurrent.ExecutionContext

class ActorExecutionContext extends ExecutionContext {

  def execute(runnable: Runnable): Unit = ???

  def reportFailure(cause: Throwable): Unit = ???
}

object ExecutionContextTest extends App {

}

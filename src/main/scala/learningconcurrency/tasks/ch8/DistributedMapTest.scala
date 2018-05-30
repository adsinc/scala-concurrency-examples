package learningconcurrency.tasks.ch8

import akka.actor.{Actor, ActorLogging, ActorRef}
import akka.pattern._
import learningconcurrency.tasks.ch8.ShardActor.{Get, GetResult, Update}

import scala.concurrent.Future

class DistributedMap[K, V](shards: ActorRef*) {
  def update(key: K, value: V): Future[Unit] = {
    // todo
    shards.head ? Update(key, value)
  }
  def get(key: K): Future[Option[V]] = ???
}

class ShardActor[K, V] extends Actor with ActorLogging {
  def receive: Receive = onMessage(Map.empty[K, V])

  def onMessage(data: Map[K, V]): Receive = {
    case Update(key: K, value: V) =>
      context become onMessage(data + (key -> value))
    case Get(key: K) =>
      GetResult(data get key)
  }
}

object ShardActor {
  case class Update[K, V](key: K, value: V)
  case class Get[K](key: K)
  case class GetResult[V](result: Option[V])
}

object DistributedMapTest extends App{

}

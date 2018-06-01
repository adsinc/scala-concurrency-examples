package learningconcurrency.tasks.ch8

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern._
import learningconcurrency.tasks.ch8.ShardActor.{Get, GetResult, Update}

import scala.concurrent.Future

class DistributedMap[K, V](shards: ActorRef*) {
  def update(key: K, value: V): Future[Unit] = {
    // todo
    shards.head ? Update(key, value) map (_ => ())
  }

  def get(key: K): Future[Option[V]] = ???
}

object DistributedMap {
  def apply[K, V](shardCount: Int)(implicit actorSystem: ActorSystem): DistributedMap[K, V] = {
    new DistributedMap {
      for (_ <- 1 to shardCount) yield actorSystem.actorOf(Props[ShardActor[K, V]])
    }
  }
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

object DistributedMapTest extends App {

  DistributedMap(10)
}

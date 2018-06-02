package learningconcurrency.tasks.ch8

import akka.actor.{Actor, ActorLogging, ActorRef, ActorSystem, Props}
import akka.pattern._
import akka.util.Timeout
import akka.util.Timeout.durationToTimeout
import learningconcurrency.tasks.ch8.ShardActor.{Get, GetResult, Update, Updated}

import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future
import scala.concurrent.duration.DurationInt
import scala.util.Random

class DistributedMap[K, V](shards: ActorRef*)(implicit getShardNumber: K => Int) {
  private implicit val timeout: Timeout = 1.second

  def update(key: K, value: V): Future[Unit] =
    findShard(key) ? Update(key, value) map (_ => ())

  def get(key: K): Future[Option[V]] =
    findShard(key) ? Get(key) collect {
      case GetResult(v: Option[V]) => v
    }

  private def findShard(key: K) = shards(calcShardNumber(key))

  private def calcShardNumber(key: K) = getShardNumber(key) % shards.size
}

object DistributedMap {
  def apply[K, V](shardCount: Int)(implicit actorSystem: ActorSystem, getShardNumber: K => Int): DistributedMap[K, V] = {
    val shards = for (i <- 1 to shardCount) yield actorSystem.actorOf(Props[ShardActor[K, V]], s"shard$i")
    new DistributedMap[K, V](shards :_*)
  }
}

class ShardActor[K, V] extends Actor with ActorLogging {
  def receive: Receive = onMessage(Map.empty[K, V])

  def onMessage(data: Map[K, V]): Receive = {
    case Update(key: K, value: V) =>
//      log.info(s"update key:$key value:$value")
      context become onMessage(data + (key -> value))
      sender() ! Updated(key)
    case Get(key: K) =>
//      log.info(s"get key:$key")
      sender() ! GetResult(data get key)
  }
}

object ShardActor {
  case class Update[K, V](key: K, value: V)
  case class Updated[K](key: K)
  case class Get[K](key: K)
  case class GetResult[V](result: Option[V])
}

object DistributedMapTest extends App {
  val numbers = 1 to 10000 map (_ => Random.nextInt(30))

  implicit val actorSystem: ActorSystem = ActorSystem("AppActorSystem")
  val m = DistributedMap[Int, Int](4)

  val updates = numbers map { n =>
    m.update(Random.nextInt(30), n)
  }

  for {
    _ <- Future.sequence(updates)
    keys = 0 until 30
    v <- Future.sequence(0 until 30 map m.get)
    ps <- keys zip v
  } println(ps)

  Thread.sleep(10000)
  actorSystem.terminate()
}

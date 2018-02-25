package learningconcurrency.tasks.ch1

import learningconcurrency.tasks.ch1.Tasks._

import scala.util.{Success, Try}

object Tasks {
  // 1
  def compose[A, B, C](f: A => B, g: B => C): A => C =
    a => g(f(a))

  // 2
  def fuse[A, B](a: Option[A], b: Option[B]): Option[(A, B)] =
    for {
      av <- a
      bv <- b
    } yield (av, bv)

  // 3
  def check[T](xs: Seq[T])(pred: T => Boolean): Boolean =
    Try(xs forall pred) match {
      case Success(res) => res
      case _ => false
    }

  class Pair[A, B](val first: A, val second: B)

  object Pair {
    def unapply[A, B](arg: Pair[A, B]): Option[(A, B)] = Some(arg.first, arg.second)
  }
}

object Test3 extends App {
  println(check(0 until 10)(40 / _ > 0))
}

object Test4 extends App {
  new Pair(10, "human") match {
    case Pair(count, kind) => println(s"$count $kind")
  }
}

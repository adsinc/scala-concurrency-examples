package learningconcurrency.tasks.ch1

import scala.util.{Success, Try}

object Tasks1 {
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

  // 4
  class Pair[A, B](val first: A, val second: B)

  object Pair {
    def unapply[A, B](arg: Pair[A, B]): Option[(A, B)] = Some(arg.first, arg.second)
  }

  // 5
  def permutations(s: String): Seq[String] =
    if (s.isEmpty) Seq.empty[String]
    else if (s.length == 1) Seq(s)
    else permutations(s.tail).flatMap { sq =>
      for {
        i <- 0 to sq.length
        (h, t) = sq.splitAt(i)
      } yield (h :+ s.head) ++ t
    }

  // 6
  def combinations(n: Int, xs: Seq[Int]): Iterator[Seq[Int]] = ???

  // 7
  def matcher(regex: String): PartialFunction[String, List[String]] =
    new PartialFunction[String, List[String]] {
      private val r = regex.r

      def isDefinedAt(x: String): Boolean = r.findFirstIn(x).isDefined

      def apply(s: String): List[String] =
        if (!isDefinedAt(s)) throw new MatchError(s"Can't match $s")
        else (r findAllIn s).toList

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

object Test5 extends App {
  println(permutations("1234"))
}

object Test7 extends App {

  val m = matcher("'.+'")

  println(m("'aaa','bbb'"))
  println(m("aaa"))
}

package com.franklinchen

/**
  Fair backtracking stream.

  http://okmij.org/ftp/Computation/monads.html#fair-bt-stream

  Basic implementation without bells and whistles such as integrating
  into the Scala collections framework or scalaz or something.

  Just enough has been provided to allow use of for-comprehensions.
  */
abstract class FairStream[+A] {
  @inline private def asFairStream[B](x: AnyRef): FairStream[B] =
    x.asInstanceOf[FairStream[B]]

  def append[B >: A](that: => FairStream[B]): FairStream[B] = this match {
    case Nil => Incomplete(that)
    case One(a) => Choice(a, that)
    case c: Choice[A] => Choice(c.head, asFairStream[B](that append c.tail))
    case i: Incomplete[A] => that match {
      case Nil => that
      case One(b) => Choice(b, i.unwrap)
      case c: Choice[B] =>
        Choice(c.head, asFairStream[B](i.unwrap append c.tail))
      case j: Incomplete[B] =>
        Incomplete(asFairStream[B](i.unwrap append j.unwrap))
    }
  }

  def flatMap[B](f: A => FairStream[B]): FairStream[B] = this match {
    case Nil => Nil
    case One(a) => f(a)
    case c: Choice[A] => f(c.head) append Incomplete(c.tail flatMap f)
    case i: Incomplete[A] => Incomplete(i.unwrap flatMap f)
  }

  /* Default implementation.
  def map[B](f: A => B): FairStream[B] = this flatMap { a => FairStream(f(a)) }
   */

  def map[B](f: A => B): FairStream[B] = this match {
    case Nil => Nil
    case One(a) => One(f(a))
    case c: Choice[A] => Choice(f(c.head), Incomplete(c.tail map f))
    case i: Incomplete[A] => Incomplete(i.unwrap map f)
  }

  def filter(p: A => Boolean): FairStream[A] = this match {
    case Nil => Nil
    case One(a) => if (p(a)) this else Nil
    case c: Choice[A] => if (p(c.head))
      Choice(c.head, c.tail filter p) else c.tail filter p
    case i: Incomplete[A] => Incomplete(i.unwrap filter p)
  }

  def withFilter(p: A => Boolean): FairStreamWithFilter =
    new FairStreamWithFilter(p)

  final class FairStreamWithFilter(p: A => Boolean) {
    def map[B](f: A => B): FairStream[B] = {
      def loop(coll: FairStream[A]): FairStream[B] = coll match {
        case Nil => Nil
        case One(a) => if (p(a)) One(f(a)) else Nil
        case c: Choice[A] => if (p(c.head))
          Choice(f(c.head), loop(c.tail)) else loop(c.tail)
        case i: Incomplete[A] => Incomplete(loop(i.unwrap))
      }

      loop(FairStream.this)
    }

    def flatMap[B](f: A => FairStream[B]): FairStream[B] = {
      def loop(coll: FairStream[A]): FairStream[B] = coll match {
        case Nil => Nil
        case One(a) => if (p(a)) f(a) else Nil
        case c: Choice[A] => if (p(c.head))
          f(c.head) append Incomplete(loop(c.tail)) else Nil
        case i: Incomplete[A] => Incomplete(loop(i.unwrap))
      }

      loop(FairStream.this)
    }
  }

  def toStream: Stream[A] = this match {
    case Nil => Stream.empty
    case One(a) => Stream(a)
    case c: Choice[A] => c.head #:: c.tail.toStream
    case i: Incomplete[A] => i.unwrap.toStream
  }

  def toList: List[A] = this match {
    case Nil => scala.collection.immutable.Nil
    case One(a) => List(a)
    case c: Choice[A] => c.head :: c.tail.toList
    case i: Incomplete[A] => i.unwrap.toList
  }
}

case object Nil extends FairStream[Nothing]

final case class One[+A](a: A) extends FairStream[A]

/** Lazy cons cell. */
final class Choice[+A](val head: A, tailThunk: => FairStream[A])
    extends FairStream[A] {
  lazy val tail = tailThunk
}

object Choice {
  def apply[A](head: A, tailThunk: => FairStream[A]) =
    new Choice(head, tailThunk)
}

final class Incomplete[+A](unwrapThunk: => FairStream[A])
    extends FairStream[A] {
  lazy val unwrap = unwrapThunk
}

object Incomplete {
  def apply[A](unwrapThunk: => FairStream[A]) = new Incomplete(unwrapThunk)
}

object FairStream {
  def empty[A]: FairStream[A] = Nil

  def apply[A](a: A): FairStream[A] = {
    One(a)
  }

  def guard(b: => Boolean): FairStream[Unit] =
    if (b) apply(()) else empty
}

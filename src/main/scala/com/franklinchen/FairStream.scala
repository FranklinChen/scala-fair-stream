package com.franklinchen

/**
  Fair backtracking stream.

  http://okmij.org/ftp/Computation/monads.html#fair-bt-stream

  Basic implementation without bells and whistles such as integrating
  into the Scala collections framework or scalaz or something.

  Just enough has been provided to allow use of for-comprehensions.
  */
abstract class FairStream[+A] {
  import FairStream._

  def append[B >: A](that: => FairStream[B]): FairStream[B] = this match {
    case Nil => incomplete(that)
    case One(a) => choice(a(), that)
    case Choice(h, t) => choice(h(), that append t())
    case Incomplete(u) => {
      val evalThat = that
      evalThat match {
        case Nil => evalThat
        case One(b) => choice(b(), u())
        case Choice(h, t) =>
          choice(h(), u() append t())
        case Incomplete(v) =>
          incomplete(u() append v())
      }
    }
  }

  def flatMap[B](f: A => FairStream[B]): FairStream[B] = this match {
    case Nil => Nil
    case One(a) => f(a())
    case Choice(h, t) => f(h()) append incomplete(t() flatMap f)
    case Incomplete(u) => incomplete(u() flatMap f)
  }

  /* Default implementation.
  def map[B](f: A => B): FairStream[B] = this flatMap { a => one(f(a)) }
   */

  def map[B](f: A => B): FairStream[B] = this match {
    case Nil => Nil
    case One(a) => one(f(a()))
    case Choice(h, t) => choice(f(h()), incomplete(t() map f))
    case Incomplete(u) => incomplete(u() map f)
  }

  def filter(p: A => Boolean): FairStream[A] = this match {
    case Nil => Nil
    case One(a) => if (p(a())) this else Nil
    case Choice(h, t) => if (p(h()))
      choice(h(), t() filter p) else t() filter p
    case Incomplete(u) => incomplete(u() filter p)
  }

  def withFilter(p: A => Boolean): FairStreamWithFilter =
    new FairStreamWithFilter(p)

  final class FairStreamWithFilter(p: A => Boolean) {
    def map[B](f: A => B): FairStream[B] = {
      def loop(coll: FairStream[A]): FairStream[B] = coll match {
        case Nil => Nil
        case One(a) => if (p(a())) one(f(a())) else Nil
        case Choice(h, t) => if (p(h()))
          choice(f(h()), loop(t())) else loop(t())
        case Incomplete(u) => incomplete(loop(u()))
      }

      loop(FairStream.this)
    }

    def flatMap[B](f: A => FairStream[B]): FairStream[B] = {
      def loop(coll: FairStream[A]): FairStream[B] = coll match {
        case Nil => Nil
        case One(a) => if (p(a())) f(a()) else Nil
        case Choice(h, t) => if (p(h()))
          f(h()) append incomplete(loop(t())) else Nil
        case Incomplete(u) => incomplete(loop(u()))
      }

      loop(FairStream.this)
    }
  }

  def toStream: Stream[A] = this match {
    case Nil => Stream.empty
    case One(a) => Stream(a())
    case Choice(h, t) => h() #:: t().toStream
    case Incomplete(u) => u().toStream
  }

  def toList: List[A] = this match {
    case Nil => scala.collection.immutable.Nil
    case One(a) => List(a())
    case Choice(h, t) => h() :: t().toList
    case Incomplete(u) => u().toList
  }
}

final case object Nil extends FairStream[Nothing]

final case class One[+A](aFunc: () => A) extends FairStream[A]

/** Lazy cons cell. */
final case class Choice[+A](headFunc: () => A, tailFunc: () => FairStream[A])
    extends FairStream[A]

final case class Incomplete[+A](unwrapFunc: () => FairStream[A])
    extends FairStream[A]

object FairStream {
  def empty[A]: FairStream[A] = Nil

  def one[A](aThunk: => A): FairStream[A] = {
    lazy val a = aThunk
    One(() => a)
  }

  /** Smart constructor for laziness. */
  def choice[A](headThunk: => A, tailThunk: => FairStream[A]) = {
    lazy val head = headThunk
    lazy val tail = tailThunk
    new Choice(() => head, () => tail)
  }

  /** Smart constructor for laziness. */
  def incomplete[A](unwrapThunk: => FairStream[A]) = {
    lazy val unwrap = unwrapThunk
    new Incomplete(() => unwrap)
  }

  def guard(b: => Boolean): FairStream[Unit] =
    if (b) one(()) else empty
}

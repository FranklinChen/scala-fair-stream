package com.franklinchen

import cats._
import cats.std.all._

// TODO Verify same sequence as Haskell version.
/**
  Fair backtracking stream.

  http://okmij.org/ftp/Computation/monads.html#fair-bt-stream

  Compare with
  https://github.com/non/cats/blob/master/core/src/main/scala/cats/data/Streaming.scala
  */
sealed abstract class FairStream[A] {
  import FairStream._

  /**
    Interleaving concatenation of streams.
    */
  def concat(that: FairStream[A]): FairStream[A] = this match {
    case Empty() => that
    case One(a) => Cons(a, that)
    case Cons(a, t) => Cons(a, Wait(Later(t concat that)))
    case Wait(next) => that match {
      case Empty() => this
      case One(a) => Cons(a, this)
      case Cons(a, t) => Cons(a, Wait(next.map(_ concat t)))
      case Wait(v) => Wait(Later(next.value concat v.value))
        //TODO Use map2
    }
  }

  def flatMap[B](f: A => FairStream[B]): FairStream[B] = this match {
    case Empty() => empty
    case One(a) => f(a)
    case Cons(a, t) => f(a).concat(Wait(Later(t flatMap f)))
    case Wait(next) => Wait(next.map(_ flatMap f))
  }

  /** Default implementation, unused. */
  def mapDefault[B](f: A => B): FairStream[B] =
    this flatMap { a => FairStream(f(a)) }

  def map[B](f: A => B): FairStream[B] = this match {
    case Empty() => empty
    case One(a) => FairStream(f(a))
    case Cons(a, t) => Cons(f(a), Wait(Later(t map f)))
    case Wait(next) => Wait(next.map(_ map f))
  }

  def filter(p: A => Boolean): FairStream[A] = this match {
    case Empty() => empty
    case One(a) => if (p(a)) this else empty
    case Cons(a, t) =>
      if (p(a))
        Cons(a, t filter p)
      else
        t filter p
    case Wait(next) => Wait(next.map(_ filter p))
  }

  def withFilter(p: A => Boolean): FairStreamWithFilter =
    new FairStreamWithFilter(p)

  final class FairStreamWithFilter(p: A => Boolean) {
    def map[B](f: A => B): FairStream[B] = {
      def loop(coll: FairStream[A]): FairStream[B] = coll match {
        case Empty() => empty
        case One(a) => if (p(a)) FairStream(f(a)) else empty
        case Cons(a, t) =>
          if (p(a))
            Cons(f(a), loop(t))
          else
            loop(t)
        case Wait(next) => Wait(next.map(loop))
      }

      loop(FairStream.this)
    }

    def flatMap[B](f: A => FairStream[B]): FairStream[B] = {
      def loop(coll: FairStream[A]): FairStream[B] = coll match {
        case Empty() => empty
        case One(a) => if (p(a)) f(a) else empty
        case Cons(a, t) =>
          if (p(a))
            f(a) concat Wait(Later(loop(t)))
          else
            empty
        case Wait(next) => Wait(next.map(loop))
      }

      loop(FairStream.this)
    }
  }

  // TODO use foldRight
  def toStream: Stream[A] = this match {
    case Empty() => Stream.empty
    case One(a) => Stream(a)
    case Cons(a, t) => a #:: t.toStream
    case Wait(next) => next.value.toStream
  }

  // TODO use foldRight
  def toList: List[A] = this match {
    case Empty() => Nil
    case One(a) => List(a)
    case Cons(a, t) => a :: t.toList
    case Wait(next) => next.value.toList
  }
}

trait FairStreamInstances {
  //TODO
  // Eq[FairStream[A]]

  // Show[FairStream[A]]

  // Foldable[FairStream]

  implicit object fairStreamMonad extends Monad[FairStream] {
    override def map[A, B](as: FairStream[A])(f: A => B): FairStream[B] =
      as.map(f)

    def pure[A](a: A): FairStream[A] =
      FairStream(a)

    def flatMap[A, B](as: FairStream[A])(f: A => FairStream[B]) : FairStream[B] =
      as.flatMap(f)
  }
}

/** The lowercase smart constructors are all by-name. */
object FairStream extends FairStreamInstances {
  /** Instead of case object because that requires covariance. */
  final case class Empty[A]() extends FairStream[A]

  final case class One[A](a: A) extends FairStream[A]

  final case class Cons[A](a: A, tail: FairStream[A])
      extends FairStream[A]

  final case class Wait[A](next: Eval[FairStream[A]])
      extends FairStream[A]

  def empty[A]: FairStream[A] =
    Empty()

  def apply[A](a: A): FairStream[A] =
    One(a)

  def cons[A](a: A, s: FairStream[A]): FairStream[A] =
    Cons(a, s)

  def wait[A](ls: Eval[FairStream[A]]): FairStream[A] =
    Wait(ls)

  def guard(condition: => Boolean): FairStream[Unit] =
    if (condition) FairStream(()) else empty
}

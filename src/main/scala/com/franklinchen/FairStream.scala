package com.franklinchen

import cats._
import cats.instances.all._
import cats.implicits._
import scala.annotation.tailrec

// TODO Verify same sequence as Haskell version.
/**
  Fair backtracking stream.

  http://okmij.org/ftp/Computation/monads.html#fair-bt-stream
  */
sealed abstract class FairStream[+A] {
  import FairStream._

  def head: A

  def tail: FairStream[A]

  final def isEmpty: Boolean = this match {
    case Empty => true
    case _ => false
  }

  @tailrec
  final def foldLeft[B](z: B)(op: (B, A) => B): B = {
    if (this.isEmpty) z
    else tail.foldLeft(op(z, head))(op)
  }

  /**
    Interleaving concatenation of streams.
    */
  final def concat[B >: A](that: FairStream[B]): FairStream[B] = this match {
    case Empty => that
    case One(a) => Cons(a, that)
    case Cons(a, t) => Cons(a, Wait(Later(t.concat(that))))
    case Wait(next) =>
      that match {
        case Empty => this
        case One(a) => Cons(a, this)
        case Cons(a, t) => Cons(a, Wait(next.map(_.concat(t))))
        case Wait(v) => Wait(Later(next.value.concat(v.value)))
        //TODO Use map2
      }
  }

  /** For for-comprehensions. */
  final def flatMap[B](f: A => FairStream[B]): FairStream[B] = this match {
    case Empty => Empty
    case One(a) => f(a)
    case Cons(a, t) => f(a).concat(Wait(Later(t.flatMap(f))))
    case Wait(next) => Wait(next.map(_.flatMap(f)))
  }

  /** For for-comprehensions. */
  final def map[B](f: A => B): FairStream[B] = this match {
    case Empty => Empty
    case One(a) => FairStream(f(a))
    case Cons(a, t) => Cons(f(a), Wait(Later(t.map(f))))
    case Wait(next) => Wait(next.map(_.map(f)))
  }

  /** For for-comprehensions. */
  final def filter(p: A => Boolean): FairStream[A] = this match {
    case Empty => Empty
    case One(a) => if (p(a)) this else Empty
    case Cons(a, t) =>
      if (p(a))
        Cons(a, t.filter(p))
      else
        t.filter(p)
    case Wait(next) => Wait(next.map(_.filter(p)))
  }

  /** For for-comprehensions. */
  final def withFilter(p: A => Boolean): FairStreamWithFilter =
    new FairStreamWithFilter(p)

  final class FairStreamWithFilter(p: A => Boolean) {

    /** For for-comprehensions. */
    final def map[B](f: A => B): FairStream[B] = {
      def loop(coll: FairStream[A]): FairStream[B] = coll match {
        case Empty => Empty
        case One(a) => if (p(a)) FairStream(f(a)) else Empty
        case Cons(a, t) =>
          if (p(a))
            Cons(f(a), loop(t))
          else
            loop(t)
        case Wait(next) => Wait(next.map(loop))
      }

      loop(FairStream.this)
    }

    /** For for-comprehensions. */
    final def flatMap[B](f: A => FairStream[B]): FairStream[B] = {
      def loop(coll: FairStream[A]): FairStream[B] = coll match {
        case Empty => Empty
        case One(a) => if (p(a)) f(a) else Empty
        case Cons(a, t) =>
          if (p(a))
            f(a).concat(Wait(Later(loop(t))))
          else
            Empty
        case Wait(next) => Wait(next.map(loop))
      }

      loop(FairStream.this)
    }
  }

  def toLazyList: LazyList[A] = this match {
    case Empty => LazyList.empty
    case One(a) => LazyList(a)
    case Cons(a, t) => a #:: t.toLazyList
    case Wait(next) => next.value.toLazyList
  }

  def toList: List[A] = this match {
    case Empty => List.empty
    case One(a) => List(a)
    case Cons(a, t) => a :: t.toList
    case Wait(next) => next.value.toList
  }
}

trait FairStreamInstances {
  //TODO more instances for standard Scala and for Cats.
  implicit val instancesForFairStream: Monad[FairStream] =
    new Monad[FairStream] {
      override def pure[A](a: A): FairStream[A] = FairStream(a)

      override def map[A, B](fa: FairStream[A])(f: A => B): FairStream[B] =
        fa.map(f)

      override def flatMap[A, B](fa: FairStream[A])(
          f: A => FairStream[B]): FairStream[B] =
        fa.flatMap(f)

      /** TODO Implement. */
      override def tailRecM[A, B](a: A)(
          f: A => FairStream[Either[A, B]]): FairStream[B] = ???
    }
}

object FairStream extends FairStreamInstances {
  case object Empty extends FairStream[Nothing] {
    def head: Nothing = throw new NoSuchElementException("head of empty fair stream")
    def tail: FairStream[Nothing] =
      throw new UnsupportedOperationException("tail of empty fair stream")
  }

  final case class One[A](head: A) extends FairStream[A] {
    def tail: FairStream[A] = Empty
  }

  final case class Cons[A](head: A, tail: FairStream[A]) extends FairStream[A]

  final case class Wait[A](next: Eval[FairStream[A]]) extends FairStream[A] {
    def head: A = next.value.head
    def tail: FairStream[A] = next.value.tail
  }

  def empty[A]: FairStream[A] =
    Empty

  def apply[A]: FairStream[A] =
    Empty

  def apply[A](a: A): FairStream[A] =
    One(a)

  def cons[A](a: A, s: FairStream[A]): FairStream[A] =
    Cons(a, s)

  def wait[A](ls: Eval[FairStream[A]]): FairStream[A] =
    Wait(ls)

  def guard(condition: => Boolean): FairStream[Unit] =
    if (condition) FairStream(()) else empty
}

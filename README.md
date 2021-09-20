# Fair backtracking stream monad, in Scala

![Continuous Integration](https://github.com/FranklinChen/scala-fair-stream/workflows/Continuous%20Integration/badge.svg)

Code translated from [Oleg's original Haskell](http://okmij.org/ftp/Computation/monads.html#fair-bt-stream).

### Notes about writing code for laziness

This library illustrates the use of the
[Cats](http://typelevel.org/cats/) library to support different
evaluation strategies, such as call-by-need. For example:

``` scala
  def map[B](f: A => B): FairStream[B] = this match {
    case Empty() => empty
    case One(a) => FairStream(f(a))
    case Cons(a, t) => Cons(f(a), Wait(Later(t map f)))
    case Wait(next) => Wait(next.map(_ map f))
  }
```

uses `Later` from [`Eval`](https://github.com/typelevel/cats/blob/master/core/src/main/scala/cats/Eval.scala).

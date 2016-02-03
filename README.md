# Fair backtracking stream monad, in Scala

[![Build Status](https://travis-ci.org/FranklinChen/scala-fair-stream.png)](https://travis-ci.org/FranklinChen/scala-fair-stream)

Code translated from [Oleg's original Haskell](http://okmij.org/ftp/Computation/monads.html#fair-bt-stream), but without being as fully lazy, for now.

### Notes about writing code for laziness

One frustration I had when writing this code was not being able to use an extended pattern-matching syntax for laziness that is supported by, say, [Standard ML of New Jersey](http://www.cs.cmu.edu/~rwh/introsml/core/lazydata.htm). Given that syntax, I would have not have had to clumsily write code like

``` scala
  def map[B](f: A => B): FairStream[B] = this match {
    case Nil => Nil
    case One(a) => one(f(a()))
    case Choice(h, t) => choice(f(h()), incomplete(t() map f))
    case Incomplete(u) => incomplete(u() map f)
  }
```

where I follow a pattern to enable laziness.

The case classes are defined using explicit functions as parameters, for use with pattern matching:

``` scala
final case class Choice[+A](headFunc: () => A, tailFunc: () => FairStream[A])
    extends FairStream[A]
```

But by convention, construction must be done with "smart constructors":

``` scala
  def choice[A](headThunk: => A, tailThunk: => FairStream[A]) = {
    lazy val head = headThunk
    lazy val tail = tailThunk
    new Choice(() => head, () => tail)
  }

```

### Macros?

I'm thinking about different possible syntax extensions for Scala that could be helpful in minimizing the boilerplate.

For reference, discussions of language syntax extensions for laziness are in the 1998 paper by Wadler, Taha, and MacQueen, ["How to add laziness to a strict language, without even being odd"](http://homepages.inf.ed.ac.uk/wadler/topics/language-design.html).

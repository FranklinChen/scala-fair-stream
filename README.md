## Fair backtracking stream monad, in Scala

[![Build Status](https://travis-ci.org/FranklinChen/scala-fair-stream.png)](https://travis-ci.org/FranklinChen/scala-fair-stream)

Code translated from [Oleg's original Haskell](http://okmij.org/ftp/Computation/monads.html#fair-bt-stream), but without being as fully lazy, for now.

### Notes about writing code for laziness

One frustration I had when writing this code was not being able to use an extended pattern-matching syntax for laziness that is supported by, say, [Standard ML of New Jersey](http://www.cs.cmu.edu/~rwh/introsml/core/lazydata.htm). Given that syntax, I would have not have had to clumsily write code like

``` scala
  def map[B](f: A => B): FairStream[B] = this match {
    case Nil => Nil
    case One(a) => One(f(a))
    case c: Choice[A] => Choice(f(c.head), Incomplete(c.tail map f))
    case i: Incomplete[A] => Incomplete(i.unwrap map f)
  }
```

where I could not use a case class and pattern matching for my classes `Choice` and `Incomplete` that take a call-by-name parameter.

With a level of indirection, by creating an explicit lazy cell, I could get pattern matching but then have to manually force it as well, so I'm thinking about different possible syntax extensions for Scala that could be helpful in minimizing the boilerplate. Scala's `lazy` construct is limited because it hides the underlying suspension.

For reference, discussions of language syntax extensions for laziness are in the 1998 paper by Wadler, Taha, and MacQueen, ["How to add laziness to a strict language, without even being odd"](http://homepages.inf.ed.ac.uk/wadler/topics/language-design.html).

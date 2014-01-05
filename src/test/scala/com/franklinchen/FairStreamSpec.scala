package com.franklinchen

import org.specs2._

class FairStreamSpec extends Specification { def is = s2"""
  FairStream

    Pythagorean triples $e1
  """

  def e1 = {
    val pythagoreanTriples = {
      import FairStream.guard

      lazy val number: FairStream[Int] =
        FairStream(0) append (number flatMap { i => FairStream(i + 1) })

      for {
        i <- number
        () <- guard(i < 10)
      } yield (i, i, i)

      for {
        i <- number
        _ <- guard(i > 0)
        j <- number
        _ <- guard(j > 0)
        k <- number
        _ <- guard(k > 0)
        _ <- guard(i*i + j*j == k*k)
      } yield (i, j, k)
    }

    pythagoreanTriples.toStream.take(7) === Seq(
      (3,4,5),
      (4,3,5),
      (6,8,10),
      (8,6,10),
      (5,12,13),
      (12,5,13),
      (9,12,15)
    )
  }
}

package com.franklinchen

import org.specs2._

import cats._

import FairStream._

class FairStreamSpec extends Specification {
  def is = s2"""
  ${`Pythagorean triples`}
  """

  def `Pythagorean triples` = {

    /**
      Infinite fair stream of natural numbers.
      */
    lazy val number: FairStream[Int] =
      FairStream(0).concat(
        FairStream.wait(Later(number flatMap { i =>
          FairStream(i + 1)
        }))
      )

    val triples = for {
      i <- number; () <- guard(i > 0)
      j <- number; () <- guard(j > 0)
      k <- number; () <- guard(k > 0)

      () <- guard(i * i + j * j == k * k)
    } yield (i, j, k)

    triples.toStream.take(7) ==== Stream(
      (3, 4, 5),
      (4, 3, 5),
      (6, 8, 10),
      (8, 6, 10),
      (5, 12, 13),
      (12, 5, 13),
      (9, 12, 15)
    )
  }
}

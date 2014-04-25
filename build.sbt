name := "scala-fair-stream"

organization := "com.franklinchen"

organizationHomepage := Some(url("http://franklinchen.com/"))

homepage := Some(url("http://github.com/FranklinChen/scala-fair-stream"))

startYear := Some(2013)

description := "Fair, backtracking monad for Scala"

version := "1.0.0"

scalaVersion := "2.11.0"

scalacOptions += "-deprecation"

libraryDependencies ++= Seq(
  "org.scalacheck" %% "scalacheck" % "1.11.3" % "test",
  "org.specs2" %% "specs2" % "2.3.11" % "test"
)

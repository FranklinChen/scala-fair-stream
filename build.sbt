name := "scala-fair-stream"

organization := "com.franklinchen"

organizationHomepage := Some(url("http://franklinchen.com/"))

homepage := Some(url("http://github.com/FranklinChen/scala-fair-stream"))

startYear := Some(2013)

description := "Fair, backtracking monad for Scala"

version := "1.0.0"

scalaVersion := "2.12.3"

scalacOptions ++= Seq(
  "-unchecked",
  "-deprecation",
  "-feature"
)

libraryDependencies ++= Seq(
  "org.typelevel" %% "cats-core" % "1.0.0-MF",
  "org.specs2" %% "specs2-core" % "3.9.4" % Test
)

ThisBuild / scalaVersion := "3.4.0"
ThisBuild / githubWorkflowPublishTargetBranches := Seq()

lazy val root = (project in file("."))
  .settings(
    name := "scala-fair-stream",
    organization := "com.franklinchen",
    organizationHomepage := Some(url("https://franklinchen.com/")),
    homepage := Some(url("https://github.com/FranklinChen/scala-fair-stream")),
    startYear := Some(2013),
    description := "Fair, backtracking monad for Scala",
    version := "1.0.0",
    crossScalaVersions := List("2.12.19", "2.13.13", "3.3.3", "3.4.0"),
    scalacOptions ++= Seq(
      "-deprecation",
      "-feature"
    ),
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats-core" % "2.10.0",
      "org.specs2" %% "specs2-core" % "5.5.1" % Test
    )
  )

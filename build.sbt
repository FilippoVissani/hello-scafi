val scafi_core  = "it.unibo.scafi" %% "scafi-core"  % "1.1.7"
val scalaLogging = "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5"
val logback = "ch.qos.logback" % "logback-classic" % "1.4.6"

ThisBuild / scalaVersion := "2.13.10"
ThisBuild / organization := "com.example"

lazy val hello = (project in file("."))
  .settings(
    name := "Hello",
    libraryDependencies ++= Seq(
      scafi_core,
      scalaLogging,
      logback,
    )
  )

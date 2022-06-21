val scafi_core  = "it.unibo.scafi" %% "scafi-core"  % "1.1.5"

ThisBuild / scalaVersion := "2.13.8"
ThisBuild / organization := "com.example"

lazy val hello = (project in file("."))
  .settings(
    name := "Hello",
    libraryDependencies ++= Seq(scafi_core)
  )

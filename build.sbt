ThisBuild / version := "0.1.0-SNAPSHOT"

ThisBuild / scalaVersion := "2.13.8"

lazy val root = (project in file("."))
  .settings(
    name := "merkelTreeImpl",
    libraryDependencies ++= Seq(
      "org.scalatest" %% "scalatest" % "3.2.15" % Test,
      "org.scalatestplus" %% "scalacheck-1-17" % "3.2.15.0" % Test,
      "org.scalacheck" %% "scalacheck" % "1.17.0" % Test
    )
  )

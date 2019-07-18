import sbt.Keys.libraryDependencies

ThisBuild / scalaVersion := "2.12.7"
ThisBuild / version := "0.1.0"
ThisBuild / organization := "io.github.iainhull"


val akkaVersion = "2.5.23"
val scalatestVersion = "3.0.5"
val scalaLoggingVersion = "3.5.0"
val akkaPersistenceInmemoryVersion = "2.5.15.2"
val slf4jVersion  = "1.7.25"
val logbackVersion = "1.2.3"

lazy val hello = (project in file("."))
  .settings(
    name := "akka-persistence-message-bug",
    libraryDependencies += "org.scalatest" %% "scalatest" % scalatestVersion % Test,
    libraryDependencies += "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
    libraryDependencies += "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % scalaLoggingVersion,
    libraryDependencies += "com.github.dnvriend" %% "akka-persistence-inmemory" % akkaPersistenceInmemoryVersion,
    libraryDependencies += "org.slf4j" % "slf4j-api" % slf4jVersion,
    libraryDependencies += "ch.qos.logback" % "logback-classic" % logbackVersion % Runtime,
  )
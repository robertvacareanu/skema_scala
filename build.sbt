name := "skema_scala"

ThisBuild / organization := "org.clulab"
ThisBuild / scalaVersion := "2.12.18"

libraryDependencies ++= {
  val luceneVersion = "9.8.0"
  val procVer = "8.1.3"
  Seq(
    "org.apache.lucene" % "lucene-core"             % "8.11.2",
    "org.apache.lucene" % "lucene-queryparser"      % "8.11.2",
    "org.apache.lucene" % "lucene-analyzers-common" % "8.11.2",
    "org.scalatest"     %% "scalatest"              % "3.0.8" % Test,
    "com.lihaoyi"       %% "upickle"                % "3.1.3",
    "com.lihaoyi"       %% "ujson"                  % "3.1.3",
    "com.typesafe"       % "config"                 % "1.4.3",
    "org.scala-lang.modules" %% "scala-parser-combinators" % "1.1.2"
  )
}
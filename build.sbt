name := "scala-grounders"

ThisBuild / organization := "org.clulab"
ThisBuild / scalaVersion := "2.12.18"

resolvers += "clulab" at "https://artifactory.clulab.org/artifactory/sbt-release"

libraryDependencies ++= {
  val luceneVersion = "9.8.0"
  val procVer = "9.0.0-RC2"
  Seq(
    "org.apache.lucene" % "lucene-core"             % "8.11.2",
    "org.apache.lucene" % "lucene-queryparser"      % "8.11.2",
    "org.apache.lucene" % "lucene-analyzers-common" % "8.11.2",
    "org.scalatest"     %% "scalatest"              % "3.0.8" % Test,
    "com.lihaoyi"       %% "upickle"                % "2.0.0",
    "com.lihaoyi"       %% "ujson"                  % "2.0.0",
    "com.typesafe"       % "config"                 % "1.4.3",
    "org.scala-lang.modules"    %% "scala-parser-combinators"   % "1.1.2",
    "org.clulab"                %% "scala-transformers-encoder" % "0.4.0",
    "com.microsoft.onnxruntime"  % "onnxruntime"             % "1.13.1",
  )
}
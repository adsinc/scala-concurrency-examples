name := "scala-concurrency-examples"

version := "1.0"

scalaVersion := "2.12.5"

// https://mvnrepository.com/artifact/commons-io/commons-io
libraryDependencies += "commons-io" % "commons-io" % "2.6"

libraryDependencies += "org.scala-lang.modules" %% "scala-async" % "0.9.7"
libraryDependencies += "io.reactivex" %% "rxscala" % "0.26.5"
libraryDependencies += "org.scala-lang.modules" %% "scala-swing" % "2.0.3"

libraryDependencies += "org.scala-stm" %% "scala-stm" % "0.8"
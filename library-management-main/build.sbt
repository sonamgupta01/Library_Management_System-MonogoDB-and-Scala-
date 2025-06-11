ThisBuild / scalaVersion     := "2.13.12"
ThisBuild / version          := "0.1.0-SNAPSHOT"
ThisBuild / organization     := "com.example"
ThisBuild / organizationName := "example"

lazy val root = (project in file("."))
  .settings(
    name := "library-management",

    // Enable forking for better performance
    Compile / run / fork := true,
    Compile / run / javaOptions += "-Xmx2G",
    Compile / run / javaOptions += "-XX:+UseG1GC",

    // MongoDB Scala Driver for database connectivity
    libraryDependencies += "org.mongodb.scala" %% "mongo-scala-driver" % "4.10.2",
    
    // HTTP4s for web server
    libraryDependencies += "org.http4s" %% "http4s-ember-server" % "0.23.25",
    libraryDependencies += "org.http4s" %% "http4s-ember-client" % "0.23.25",
    libraryDependencies += "org.http4s" %% "http4s-circe" % "0.23.25",
    libraryDependencies += "org.http4s" %% "http4s-dsl" % "0.23.25",
    
    // Circe for JSON handling
    libraryDependencies += "io.circe" %% "circe-generic" % "0.14.6",
    libraryDependencies += "io.circe" %% "circe-parser" % "0.14.6",
    
    // PureConfig for configuration
    libraryDependencies += "com.github.pureconfig" %% "pureconfig" % "0.17.5",
    
    // Logging
    libraryDependencies += "ch.qos.logback" % "logback-classic" % "1.4.14",
    libraryDependencies += "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",

    // MUnit for testing
    libraryDependencies += "org.scalameta" %% "munit" % "0.7.29" % Test
  )

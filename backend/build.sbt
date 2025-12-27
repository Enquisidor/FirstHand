name := "firsthand-backend"

version := "0.1.0"

scalaVersion := "2.13.12"

lazy val akkaVersion = "2.8.5"
lazy val akkaHttpVersion = "10.5.3"

libraryDependencies ++= Seq(
  // Akka Actor
  "com.typesafe.akka" %% "akka-actor-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-stream" % akkaVersion,

  // Akka HTTP
  "com.typesafe.akka" %% "akka-http" % akkaHttpVersion,
  "com.typesafe.akka" %% "akka-http-spray-json" % akkaHttpVersion,

  // Akka Persistence
  "com.typesafe.akka" %% "akka-persistence-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-typed" % akkaVersion,
  "com.typesafe.akka" %% "akka-cluster-sharding-typed" % akkaVersion,

  // Firebase Admin SDK
  "com.google.firebase" % "firebase-admin" % "9.2.0",

  // Google Cloud
  "com.google.cloud" % "google-cloud-firestore" % "3.14.5",
  "com.google.cloud" % "google-cloud-storage" % "2.29.1",

  // JSON
  "io.spray" %% "spray-json" % "1.3.6",
  "com.typesafe.play" %% "play-json" % "2.10.4",

  // Logging
  "ch.qos.logback" % "logback-classic" % "1.4.14",
  "com.typesafe.scala-logging" %% "scala-logging" % "3.9.5",

  // Configuration
  "com.typesafe" % "config" % "1.4.3",

  // Testing
  "com.typesafe.akka" %% "akka-actor-testkit-typed" % akkaVersion % Test,
  "com.typesafe.akka" %% "akka-http-testkit" % akkaHttpVersion % Test,
  "org.scalatest" %% "scalatest" % "3.2.17" % Test
)

enablePlugins(JavaAppPackaging)

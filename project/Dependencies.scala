import sbt._

object Dependencies {
  object Versions {
    val akkaHttp        = "10.1.1"
    val akkaStream      = "2.5.13"
    val akkaHttpCirce   = "1.20.1"
    val circe           = "0.9.3"
    val scalatest       = "3.0.5"
    val pureconfig      = "0.9.1"
    val logbackClassic  = "1.2.3"
    val scalaLogging    = "3.9.0"
    val doobie          = "0.5.3"
    val aerospike       = "4.0.2"
    val flyway          = "4.2.0"
  }

  val akkaHttp = Seq(
    "com.typesafe.akka"     %% "akka-http"         % Versions.akkaHttp,
    "com.typesafe.akka"     %% "akka-stream"       % Versions.akkaStream
  )

  val circe = Seq(
    "io.circe"              %% "circe-core"                       % Versions.circe,
    "io.circe"              %% "circe-generic"                    % Versions.circe,
    "io.circe"              %% "circe-parser"                     % Versions.circe,
    "de.heikoseeberger"     %% "akka-http-circe"                  % Versions.akkaHttpCirce
  )

  val pureconfig      = "com.github.pureconfig" %% "pureconfig"        % Versions.pureconfig
  val logging = Seq (
    "ch.qos.logback"             %  "logback-classic"   % Versions.logbackClassic,
    "com.typesafe.scala-logging" %% "scala-logging"     % Versions.scalaLogging
  )

  val doobie = Seq(
    "org.tpolecat" %% "doobie-core"     % Versions.doobie,
    "org.tpolecat" %% "doobie-postgres" % Versions.doobie
  )

  val aerospike = "com.aerospike" % "aerospike-client" % Versions.aerospike

  val akkaHttpTestkit = "com.typesafe.akka"     %% "akka-http-testkit" % Versions.akkaHttp  % "test"
  val scalatest       = "org.scalatest"         %% "scalatest"         % Versions.scalatest % "test"

  val flyway          = "org.flywaydb"          % "flyway-core"        % Versions.flyway    % "test"

}
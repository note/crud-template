import com.typesafe.sbt.GitPlugin.autoImport._
import sbt._
import Common._
import Versions._

name := """crud-template"""

version := "1.0"

lazy val root = (project in file("."))
  .commonSettings
  .aggregate(core, web)

lazy val web = (project in file("web"))
  .enablePlugins(BuildInfoPlugin, GitVersioning, GitBranchPrompt)
  .commonSettings
  .settings(
    // see more about BuildInfoPlugin and SbtGit at http://blog.byjean.eu/2015/07/10/painless-release-with-sbt.html
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, git.baseVersion, git.gitHeadCommit),
    buildInfoPackage := "net.michalsitko",
    buildInfoUsePackageAsPath := true,
    libraryDependencies ++= Seq(
      "com.typesafe.akka"     %% "akka-http"                        % akkaHttpVersion,
      "io.circe"              %% "circe-core"                       % circeVersion,
      "io.circe"              %% "circe-generic"                    % circeVersion,
      "io.circe"              %% "circe-parser"                     % circeVersion,
      "de.heikoseeberger"     %% "akka-http-circe"                  % "1.14.0",
      "com.github.melrief"    %% "pureconfig"                       % "0.6.0",
      "com.typesafe.akka"     %% "akka-http-testkit"                % akkaHttpVersion % "test"
    ) ++ Common.commonDeps
  )
  .dependsOn(core)

lazy val core = (project in file("core"))
  .enablePlugins(GitBranchPrompt)
  .commonSettings
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel"              %% "cats"           % catsVersion,
      "com.softwaremill.quicklens" %% "quicklens"      % "1.4.8",
      "ch.qos.logback"             % "logback-classic" % "1.1.7",
      "com.typesafe.scala-logging" %% "scala-logging"  % "3.5.0"
    ) ++ Common.commonDeps
  )

lazy val gatlingDeps = Seq(
  "io.gatling.highcharts" % "gatling-charts-highcharts" % gatlingVersion,
  "io.gatling"            % "gatling-test-framework"    % gatlingVersion
)

lazy val gatling = (project in file("gatling"))
  .enablePlugins(GatlingPlugin)
  .commonSettings
  .settings(
    libraryDependencies ++= gatlingDeps
  )
  .dependsOn(web % "compile->compile")

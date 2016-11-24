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
      "com.typesafe.akka" %% "akka-http-experimental"            % akkaVersion,
    // TODO: remove
      "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaVersion,
      "io.circe"          %% "circe-core"                        % circeVersion,
      "io.circe"          %% "circe-generic"                     % circeVersion,
      "io.circe"          %% "circe-parser"                      % circeVersion,
      // TODO: update to stable. as for now stable version uses cats 0.5.2 and we want to use the newest cats
      "de.heikoseeberger" %% "akka-http-circe"                   % "1.11.0-M2",
      "com.typesafe.akka"     %% "akka-http-experimental"        % akkaVersion % "test",
      "io.gatling.highcharts" % "gatling-charts-highcharts"      % "2.2.3"     % "test",
      "io.gatling"            % "gatling-test-framework"         % "2.2.3"     % "test"
    ) ++ Common.commonDeps
  )
  .dependsOn(core)

lazy val core = (project in file("core"))
  .enablePlugins(GitBranchPrompt)
  .commonSettings
  .settings(
    libraryDependencies ++= Seq(
      "org.typelevel" %% "cats" % "0.8.1"
    ) ++ Common.commonDeps
  )

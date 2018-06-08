import com.typesafe.sbt.GitPlugin.autoImport._
import sbt._
import Dependencies._
import Settings._

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
    libraryDependencies ++= akkaHttp ++ circe ++ Seq(pureconfig, akkaHttpTestkit, scalatest)
  )
  .dependsOn(core)

lazy val core = (project in file("core"))
  .enablePlugins(GitBranchPrompt)
  .commonSettings
  .settings(
    libraryDependencies ++= logging ++ Seq(monix)
  )

lazy val gatling = (project in file("gatling"))
  .enablePlugins(GatlingPlugin)
  .commonSettings
  .settings(
    // there's no gatling published built with scala 2.12
    scalaVersion := "2.11.11",
    libraryDependencies ++= Dependencies.gatling ++ Seq(pureconfig)
  )

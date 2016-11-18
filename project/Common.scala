import com.typesafe.sbt.GitPlugin.autoImport._
import sbt._
import sbt.Keys._
import Versions._

object Common {
  val commonScalacOptions = Seq(
    "-target:jvm-1.8",
    "-encoding", "UTF-8",
    "-unchecked",
    "-deprecation",
    "-feature",
    "-language:existentials",
    "-language:higherKinds",
    "-language:implicitConversions",
    "-Yno-adapted-args",
    "-Ywarn-dead-code",
    "-Ywarn-infer-any",
    "-Ywarn-unused-import",
    "-Xfatal-warnings",
    "-Xlint"
  )

  val commonScalaVersion =  "2.11.8"

  implicit class ProjectFrom(project: Project) {
    def commonSettings: Project = project.settings(
      scalacOptions ++= commonScalacOptions,
      scalaVersion := commonScalaVersion,

      // git
      git.useGitDescribe := true,
      showCurrentGitBranch,

      // ammonite
      initialCommands in (Test, console) := """ammonite.Main().run()"""
    )
  }

  val commonDeps = Seq(
    "com.lihaoyi"     % "ammonite"             % "0.8.0"          % "test" cross CrossVersion.full,
    "org.scalatest"   %% "scalatest"           % scalatestVersion % "test"
  )
}
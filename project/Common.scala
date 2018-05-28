import com.typesafe.sbt.GitPlugin.autoImport._
import sbt._
import sbt.Keys._
import scoverage.ScoverageKeys._
import scoverage.ScoverageSbtPlugin
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

  val commonScalaVersion = "2.12.6"

  implicit class ProjectFrom(project: Project) {
    def commonSettings: Project = project.settings(
      scalacOptions ++= commonScalacOptions,
      scalaVersion := commonScalaVersion,

      // git
      git.useGitDescribe := true,
      showCurrentGitBranch,
      addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6"),
    )
  }

  val commonDeps = Seq(
    "org.scalatest"   %% "scalatest"           % scalatestVersion % "test"
  )
}
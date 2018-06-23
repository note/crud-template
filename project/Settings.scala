import com.typesafe.config.ConfigFactory
import com.typesafe.sbt.GitPlugin.autoImport._
import sbt.Keys._
import sbt._

object Settings {
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
      addCompilerPlugin("org.spire-math" %% "kind-projector" % "0.9.6")
    )
  }

  def flywayConfigFromFile(file: File) = {

    lazy val config = ConfigFactory.parseFile(file).resolve().getConfig("db")

    lazy val dbUrl       = config.getString("url")
    lazy val dbUser      = config.getString("user")
    lazy val dbPassword  = config.getString("password")

    DbConfig(dbUrl, dbUser, dbPassword)
  }

  final case class DbConfig(url: String, user: String, password: String)
}
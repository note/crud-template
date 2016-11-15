import sbt._
import Versions._

name := """crud-template"""

version := "1.0"

scalaVersion := "2.11.8"

// Uncomment to use Akka
libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-http-experimental"            % akkaVersion,
  "com.typesafe.akka" %% "akka-http-spray-json-experimental" % akkaVersion,
  "io.circe"          %% "circe-core"                        % circeVersion,
  "io.circe"          %% "circe-generic"                     % circeVersion,
  "io.circe"          %% "circe-parser"                      % circeVersion,
  "de.heikoseeberger" %% "akka-http-circe"                   % "1.10.1",
  "org.scalatest"     %% "scalatest"                         % "3.0.0"      % "test"
)

// git
git.useGitDescribe := true

showCurrentGitBranch

// see more about BuildInfoPlugin and SbtGit at http://blog.byjean.eu/2015/07/10/painless-release-with-sbt.html
lazy val root = (project in file(".")).
  enablePlugins(BuildInfoPlugin, GitVersioning, GitBranchPrompt).
  settings(
    buildInfoKeys := Seq[BuildInfoKey](name, version, scalaVersion, sbtVersion, git.baseVersion, git.gitHeadCommit),
    buildInfoPackage := "net.michalsitko",
    buildInfoUsePackageAsPath := true
  )

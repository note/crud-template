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
    buildInfoPackage := "tu.lambda",
    buildInfoUsePackageAsPath := true,
    libraryDependencies ++= akkaHttp ++ circe ++ Seq(pureconfig, akkaHttpTestkit, scalatest)
  )
  .dependsOn(core)

// TODO: unhardcode
lazy val flywayCfg = flywayConfigFromFile(new File("web/src/main/resources/application.conf"))

lazy val core = (project in file("core"))
  .enablePlugins(GitBranchPrompt)
  .commonSettings
  .settings(
    libraryDependencies ++= logging ++ Seq(monix, aerospike) ++ doobie ++ Seq(scalatest),
    flywayUrl := flywayCfg.url,
    flywayUser := flywayCfg.user,
    flywayPassword := flywayCfg.password,
    flywayLocations += "db/migration"
  )
  .enablePlugins(FlywayPlugin)


addCommandAlias("flywayMigrate", "core/flywayMigrate")

// alias for sbt-revolver commands. Since core is not runnable, web should be assumed for all of them
addCommandAlias("reStart", "web/reStart")
addCommandAlias("reStop", "web/reStop")
addCommandAlias("reStatus", "web/reStatus")

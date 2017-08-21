package net.michalsitko.controllers

import akka.http.scaladsl.HttpExt
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Encoder
import net.michalsitko.BuildInfo

class VersionController(http: HttpExt) extends AnyRef with FailFastCirceSupport with RequestBuilding {

  val route =
    path("version") {
      get {
        complete {
          println("hello. i am client")
          BuildInfo
        }
      }
    }

  private implicit val encodeBuildInfo: Encoder[BuildInfo.type] = { buildInfo: BuildInfo.type =>
    import io.circe.syntax._

    val fieldsMap = Map("name" -> buildInfo.name, "version" -> buildInfo.version, "scalaVersion" -> buildInfo.scalaVersion) ++
      buildInfo.gitHeadCommit.fold(Map.empty[String, String])(c => Map("commit" -> c))
    fieldsMap.asJson
  }
}

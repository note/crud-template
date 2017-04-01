package net.michalsitko.controllers

import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.{ Encoder, Json }
import net.michalsitko.BuildInfo

class VersionController(implicit materializer: ActorMaterializer) extends AnyRef with FailFastCirceSupport {

  val route =
    path("version") {
      get {
        complete(BuildInfo)
      }
    }

  private implicit val encodeBuildInfo: Encoder[BuildInfo.type] = new Encoder[BuildInfo.type] {
    import io.circe.syntax._

    final def apply(buildInfo: BuildInfo.type): Json = {
      val fieldsMap = Map("name" -> buildInfo.name, "version" -> buildInfo.version, "scalaVersion" -> buildInfo.scalaVersion) ++
        buildInfo.gitHeadCommit.fold(Map.empty[String, String])(c => Map("commit" -> c))
      fieldsMap.asJson
    }
  }
}

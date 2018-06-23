package tu.lambda.routes

import akka.http.scaladsl.server.Directives._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Encoder

class VersionRoute extends AnyRef with FailFastCirceSupport {

  val route =
    path("version") {
      get {
        complete(BuildInfo)
      }
    }

  private implicit val encodeBuildInfo: Encoder[BuildInfo.type] = { buildInfo: BuildInfo.type =>
    import io.circe.syntax._

    val fieldsMap = Map("name" -> buildInfo.name, "version" -> buildInfo.version, "scalaVersion" -> buildInfo.scalaVersion) ++
      buildInfo.gitHeadCommit.fold(Map.empty[String, String])(c => Map("commit" -> c))
    fieldsMap.asJson
  }
}

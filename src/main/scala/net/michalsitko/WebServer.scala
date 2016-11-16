package net.michalsitko

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.{ Encoder, Json }
import io.circe.syntax._

object WebServer extends AnyRef with CirceSupport {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("crud-template-http-system")
    implicit val materializer = ActorMaterializer()

    val route =
      path("version") {
        get {
          complete(BuildInfo)
        }
      }

    Http().bindAndHandle(route, "localhost", 8080)
  }

  private implicit val encodeBuildInfo: Encoder[BuildInfo.type] = new Encoder[BuildInfo.type] {
    final def apply(buildInfo: BuildInfo.type): Json = {
      val fieldsMap = Map("name" -> buildInfo.name, "version" -> buildInfo.version, "scalaVersion" -> buildInfo.scalaVersion) ++
        buildInfo.gitHeadCommit.fold(Map.empty[String, String])(c => Map("commit" -> c))
      fieldsMap.asJson
    }
  }
}

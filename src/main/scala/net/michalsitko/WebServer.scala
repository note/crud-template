package net.michalsitko

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer

object WebServer {
  def main(args: Array[String]) {

    implicit val system = ActorSystem("my-system")
    implicit val materializer = ActorMaterializer()

    val route =
      path("version") {
        get {
          val buildInfo = net.michalsitko.BuildInfo.toString
          complete(HttpEntity(ContentTypes.`text/html(UTF-8)`, buildInfo))
        }
      }

    Http().bindAndHandle(route, "localhost", 8080)
  }
}

package net.michalsitko

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import de.heikoseeberger.akkahttpcirce.CirceSupport
import net.michalsitko.controllers.{ UserController, VersionController }
import net.michalsitko.crud.service.impl.InMemoryUserService

object WebServer extends AnyRef with CirceSupport with Services {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("crud-template-http-system")
    implicit val materializer = ActorMaterializer()

    // TODO: Check if services/controllers should use ActorSystem's EC
    implicit val ec = system.dispatcher

    val versionController = new VersionController
    val userController = new UserController(userService)

    val route = versionController.route ~ userController.route

    Http().bindAndHandle(route, "localhost", 8080)
  }
}

trait Services {
  val userService = new InMemoryUserService
}

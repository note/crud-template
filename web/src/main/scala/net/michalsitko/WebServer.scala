package net.michalsitko

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import net.michalsitko.config.AppConfig
import net.michalsitko.controllers.{ UserController, VersionController }
import net.michalsitko.crud.service.impl.InMemoryUserService
import pureconfig._

import scala.util.{ Failure, Success }

object WebServer extends AnyRef with Services with StrictLogging {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("crud-template-http-system")
    implicit val materializer = ActorMaterializer()

    // TODO: Check if services/controllers should use ActorSystem's EC
    implicit val ec = system.dispatcher

    logger.info("Initializing application ...")

    val config = loadConfig[AppConfig]("app").right.get

    val route = {
      val versionController = new VersionController
      val userController = new UserController(userService)

      versionController.route ~ userController.route
    }

    val bindRes = Http().bindAndHandle(route, config.binding.host, config.binding.port)

    bindRes.onComplete {
      case Success(binding) => logger.info(s"Application listens on: ${binding.localAddress}")
      case Failure(ex) => logger.error(s"Application failed to bind to ${config.binding}", ex)
    }
  }
}

trait Services {
  val userService = new InMemoryUserService
}

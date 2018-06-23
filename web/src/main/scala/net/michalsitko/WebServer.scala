package net.michalsitko

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import com.typesafe.scalalogging.StrictLogging
import monix.execution.Scheduler
import net.michalsitko.config.AppConfig
import net.michalsitko.crud.service.impl.InMemoryUserService
import net.michalsitko.routes.{ UserRoute, VersionRoute }
import pureconfig._
import doobie.implicits._
import net.michalsitko.crud.dao.UserDao
import net.michalsitko.crud.db.DbTransactor

import scala.util.{ Failure, Success }

object WebServer extends AnyRef with Services with StrictLogging {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("crud-template-http-system")
    implicit val materializer = ActorMaterializer()

    // TODO: Check if services/controllers should use ActorSystem's EC
    implicit val ec = system.dispatcher

    logger.info("Initializing application ...")

    val config = loadConfig[AppConfig].right.get

    val route = {
      implicit val scheduler = Scheduler(system.dispatcher)

      val versionRoute = new VersionRoute
      val userRoute = new UserRoute(userService)

      versionRoute.route ~ userRoute.route
    }

    val bindRes = Http().bindAndHandle(route, config.binding.host, config.binding.port)

    bindRes.onComplete {
      case Success(binding) => logger.info(s"Application listens on: ${binding.localAddress}")
      case Failure(ex) => logger.error(s"Application failed to bind to ${config.binding}", ex)
    }

    val program2 = UserDao.program1
    val transactor = DbTransactor.transactor(config.db)
    val io = program2.transact(transactor)

    val r = io.unsafeRunSync()

    println("res from db: " + r)
  }
}

trait Services {
  val userService = new InMemoryUserService
}

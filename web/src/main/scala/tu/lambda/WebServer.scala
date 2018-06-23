package tu.lambda

import java.sql.Connection

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import cats.data.{Kleisli, NonEmptyList}
import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import pureconfig._
import tu.lambda.WebServer.{userDao, uuidGen}
import tu.lambda.config.AppConfig
import tu.lambda.crud.dao.{UUIDGenerator, UserDao}
import tu.lambda.crud.db.DbTransactor
import tu.lambda.crud.entity.{SavedUser, User}
import tu.lambda.crud.service.UserService
import tu.lambda.crud.service.impl.DbUserService
import tu.lambda.http.RoutesRequestWrapper
import tu.lambda.routes.{UserRoute, VersionRoute}

import scala.util.{Failure, Success}

object WebServer extends AnyRef with Services with StrictLogging with RoutesRequestWrapper {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("crud-template-http-system")
    implicit val materializer = ActorMaterializer()

    // TODO: Check if services/controllers should use ActorSystem's EC
    implicit val ec = system.dispatcher

    logger.info("Initializing application ...")

    val route = {
      val versionRoute = new VersionRoute
      val userRoute = new UserRoute(userService)

      requestWrapper(versionRoute.route ~ userRoute.route)
    }

    val bindRes = Http().bindAndHandle(route, config.binding.host, config.binding.port)

    bindRes.onComplete {
      case Success(binding) => logger.info(s"Application listens on: ${binding.localAddress}")
      case Failure(ex) => logger.error(s"Application failed to bind to ${config.binding}", ex)
    }close a
  }
}

trait Services {
  val config = loadConfig[AppConfig].right.get

  implicit val uuidGen = UUIDGenerator.default
  implicit val transactor = DbTransactor.transactor(config.db)

  val userService = new UserService {
    override def save(user: User) = DbUserService.save(userDao, uuidGen)(user)

    override def getByCredentials(email: String, password: String) = DbUserService.getByCredentials(userDao)(email, password)
  }

  val userDao = UserDao


}
package tu.lambda

import java.util.UUID

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Directives._
import akka.stream.ActorMaterializer
import cats.data.{EitherT, Kleisli}
import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import doobie.util.transactor.Transactor
import pureconfig._
import tu.lambda.config.AppConfig
import tu.lambda.crud.aerospike.{AerospikeClient, UserSession, UserSessionRepo}
import tu.lambda.crud.dao.{BookmarkDao, UUIDGenerator, UserDao}
import tu.lambda.crud.db.DbTransactor
import tu.lambda.crud.entity._
import tu.lambda.crud.service.impl.{AppContext, BookmarkError, DbBookmarkService, DbUserService}
import tu.lambda.crud.service.{BookmarkService, UserService}
import tu.lambda.http.RoutesRequestWrapper
import tu.lambda.routes.{BookmarkRoute, UserRoute, VersionRoute}

import scala.util.{Failure, Success}

object WebServer extends AnyRef with Services with StrictLogging with RoutesRequestWrapper {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("crud-template-http-system")
    implicit val materializer = ActorMaterializer()

    // TODO: Check if services/controllers should use ActorSystem's EC
    implicit val ec = system.dispatcher

    logger.info("Initializing application ...")

    lazy val route = {
      val versionRoute = new VersionRoute
      val userRoute = new UserRoute(userService)
      val bookmarkRoute = new BookmarkRoute(bookmarkService)

      requestWrapper(versionRoute.route ~ userRoute.route ~ bookmarkRoute.route)
    }

    val bindRes = Http().bindAndHandle(route, config.binding.host, config.binding.port)

    bindRes.onComplete {
      case Success(binding) => logger.info(s"Application listens on: ${binding.localAddress}")
      case Failure(ex) => logger.error(s"Application failed to bind to ${config.binding}", ex)
    }
  }
}

trait Services {
  val config = loadConfig[AppConfig].right.get

  implicit val uuidGen = UUIDGenerator.default
  implicit val transactor: Transactor[IO] = DbTransactor.transactor(config.db)
  val aerospikeClient = new AerospikeClient(config.aerospike)
  implicit val appContext: AppContext = AppContext(transactor, aerospikeClient)

  val userDao = UserDao
  val bookmarkDao = BookmarkDao
  val sessionRepo = UserSessionRepo

  val userService = new UserService {
    override def save(user: User) = DbUserService.save(userDao, uuidGen)(user)

    override def login(email: String, password: String): Kleisli[IO, AppContext, Option[UserSession]] =
      DbUserService.login(userDao, sessionRepo, uuidGen)(email, password).mapF(_.value)
  }

  val bookmarkService = new BookmarkService {
    override def save(bookmark: Bookmark, token: UUID): Kleisli[EitherT[IO, BookmarkError, ?], AppContext, SavedBookmark] =
      new DbBookmarkService(bookmarkDao, sessionRepo)(uuidGen).save(bookmark, token)

    override def getByUserId(token: UUID) =
      new DbBookmarkService(bookmarkDao, sessionRepo)(uuidGen).getByUserId(token)
  }



}

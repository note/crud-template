package tu.lambda.routes

import java.sql.Connection

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.data.Kleisli
import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import doobie.util.transactor.Transactor.Aux
import io.circe.Json
import io.circe.syntax._
import tu.lambda.crud.entity.User
import tu.lambda.crud.service.UserService
import tu.lambda.entity.Credentials

import scala.concurrent.Future
import scala.util.{Failure, Success}

// TODO: move it somewhere
object Helper {
  implicit class ToFuture[T](thunk: Kleisli[IO, Connection, T]) {
    def exec(implicit transactor: Aux[IO, Unit]): Future[T] = transactor.exec.apply(thunk).unsafeToFuture()
  }
}

class UserRoute(userService: UserService)(implicit transactor: Aux[IO, Unit])
  extends StrictLogging with FailFastCirceSupport {
  import tu.lambda.format.Formats._
  import Helper._

  val route: Route =
    pathPrefix("user") {
      post {
        entity(as[User]) { user =>
          // TODO: check if runAsync is how we want to run the code
          onComplete(userService.save(user).exec) {
            case Success(Right(savedUser)) =>
              complete(StatusCodes.Created -> savedUser)
            case Success(Left(errors)) =>
              val msgs = errors.map(_.message)
              val msgsJson = Json.obj("messages" -> msgs.toList.asJson)
              complete(StatusCodes.BadRequest -> msgsJson)
            // TODO: probably should be solved somewhere else
            case Failure(ex) =>
              logger.error("error", ex)
              complete(HttpResponse(StatusCodes.InternalServerError, entity = ex.getMessage))
          }
        }
      } ~
        path("login") {
          post {
            entity(as[Credentials]) { credentials =>
              // TODO: check if runAsync is how we want to run the code
              onComplete(userService.getByCredentials(credentials.email, credentials.password).exec) {
                case Success(Some(user)) =>
                  complete(user)
                case Success(None) =>
                  complete(StatusCodes.NotFound)
                // TODO: probably should be solved somewhere else
                case Failure(ex) =>
                  logger.error("error", ex)
                  complete(HttpResponse(StatusCodes.InternalServerError, entity = ex.getMessage))
              }
            }
          }
        }
    }
}

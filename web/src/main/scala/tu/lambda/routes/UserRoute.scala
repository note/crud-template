package tu.lambda.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import cats.effect.IO
import doobie.util.transactor.Transactor
import io.circe.Json
import io.circe.syntax._
import tu.lambda.crud.AppContext
import tu.lambda.crud.entity.User
import tu.lambda.crud.service.UserService
import tu.lambda.entity.Credentials

import scala.concurrent.duration.Duration


class UserRoute(userService: UserService, tokenExpiration: Duration)(implicit appContext: AppContext)
  extends BaseRoute {

  private implicit val transactor: Transactor[IO] = appContext.transactor

  val route: Route =
    pathPrefix("users") {
      post {
        entity(as[User]) { user =>
          onSuccess(userService.save(user).exec) {
            case Right(savedUser) =>
              complete(StatusCodes.Created -> savedUser)
            case Left(errors) =>
              val msgs = errors.map(_.message)
              val msgsJson = Json.obj("messages" -> msgs.toList.asJson)
              complete(StatusCodes.BadRequest -> msgsJson)
          }
        }
      } ~
        path("login") {
          post {
            entity(as[Credentials]) { credentials =>
              onSuccess(login(credentials.email, credentials.password).exec) {
                case Some(user) =>
                  complete(user)
                case None =>
                  complete(StatusCodes.NotFound)
              }
            }
          }
        }
    }

  // Example of currying and partial application
  private val login = userService.login(tokenExpiration)_
}

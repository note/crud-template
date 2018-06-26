package tu.lambda.routes

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import cats.effect.IO
import doobie.util.transactor.Transactor.Aux
import io.circe.Json
import io.circe.syntax._
import tu.lambda.crud.entity.User
import tu.lambda.crud.service.UserService
import tu.lambda.entity.Credentials


class UserRoute(userService: UserService)(implicit transactor: Aux[IO, Unit])
  extends BaseRoute {

  val route: Route =
    pathPrefix("users") {
      post {
        entity(as[User]) { user =>
          // TODO: check if runAsync is how we want to run the code
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
              // TODO: check if runAsync is how we want to run the code
              onSuccess(userService.getByCredentials(credentials.email, credentials.password).exec) {
                case Some(user) =>
                  complete(user)
                case None =>
                  complete(StatusCodes.NotFound)
              }
            }
          }
        }
    }
}

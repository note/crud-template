package tu.lambda.routes

import java.util.UUID

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import cats.data.Validated.{Invalid, Valid}
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import io.circe.syntax._
import monix.execution.Scheduler
import tu.lambda.crud.entity.{User, UserId}
import tu.lambda.crud.service.UserService

import scala.util.{Failure, Success}

class UserRoute(userService: UserService)(implicit scheduler: Scheduler)
  extends AnyRef with FailFastCirceSupport {
  import tu.lambda.format.Formats._

  val route =
    pathPrefix("user") {
      post {
        entity(as[User]) { user =>
          // TODO: check if runAsync is how we want to run the code
          onComplete(userService.save(user).runAsync) {
            case Success(Valid(savedUser)) =>
              complete(StatusCodes.Created -> savedUser)
            case Success(Invalid(errors)) =>
              val msgs = errors.map(_.message)
              val msgsJson = Json.obj("messages" -> msgs.toList.asJson)
              complete(StatusCodes.BadRequest -> msgsJson)
            case Failure(e) =>
              complete(HttpResponse(StatusCodes.InternalServerError, entity = e.getMessage))
          }
        }
      } ~
        path(Remaining) { userId =>
          get {
            // TODO: check if runAsync is how we want to run the code
            onComplete(userService.get(UserId(UUID.fromString(userId))).runAsync) {
              case Success(Some(user)) =>
                complete(user)
              case Success(None) =>
                complete(StatusCodes.NotFound)
              case Failure(e) =>
                complete(HttpResponse(StatusCodes.InternalServerError, entity = e.getMessage))
            }

          }
        }
    }
}

package net.michalsitko.controllers

import java.util.UUID

import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import cats.data.Validated.{ Invalid, Valid }
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import io.circe.generic.auto._
import io.circe.syntax._
import net.michalsitko.crud.entity.{ User, UserId }
import net.michalsitko.crud.service.UserService

import scala.util.{ Failure, Success }

class UserController(userService: UserService)
  extends AnyRef with FailFastCirceSupport {
  import net.michalsitko.format.Formats._

  val route =
    pathPrefix("user") {
      post {
        entity(as[User]) { user =>
          onComplete(userService.save(user)) {
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
            onComplete(userService.get(UserId(UUID.fromString(userId)))) {
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

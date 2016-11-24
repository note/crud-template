package net.michalsitko.controllers

import java.util.UUID

import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
import akka.http.scaladsl.server.Directives._
import cats.data.Validated.{ Invalid, Valid }
import de.heikoseeberger.akkahttpcirce.CirceSupport
import net.michalsitko.crud.entity.{ UserId, User }
import net.michalsitko.crud.service.UserService

import io.circe.generic.auto._

import scala.concurrent.ExecutionContext
import scala.util.{ Success, Failure }

class UserController(userService: UserService)(implicit ec: ExecutionContext)
    extends AnyRef with CirceSupport {

  import net.michalsitko.format.Formats._

  val route =
    pathPrefix("user") {
      post {
        entity(as[User]) { user =>
          onComplete(userService.save(user)) {
            case Success(Valid(savedUser)) =>
              complete(savedUser)
            case Success(Invalid(errors)) =>
              val msgs = errors.map(_.message)
              complete(HttpResponse(StatusCodes.BadRequest, entity = msgs.toList.mkString(", ")))
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

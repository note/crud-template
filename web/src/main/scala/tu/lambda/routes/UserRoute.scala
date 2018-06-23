package tu.lambda.routes

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import doobie.util.transactor.Transactor.Aux
import io.circe.Json
import io.circe.syntax._
import tu.lambda.crud.entity.User
import tu.lambda.crud.service.UserService

import scala.util.{Failure, Success}

class UserRoute(userService: UserService)(implicit transactor: Aux[IO, Unit])
  extends StrictLogging with FailFastCirceSupport {
  import tu.lambda.format.Formats._

  val route: Route =
    pathPrefix("user") {
      post {
        entity(as[User]) { user =>
          // TODO: check if runAsync is how we want to run the code
          onComplete(transactor.exec.apply(userService.save(user)).unsafeToFuture()) {
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
      }
//        path(Remaining) { userId =>
//          get {
//            // TODO: check if runAsync is how we want to run the code
//            onComplete(userService.get(UserId(UUID.fromString(userId))).runAsync) {
//              case Success(Some(user)) =>
//                complete(user)
//              case Success(None) =>
//                complete(StatusCodes.NotFound)
//              case Failure(e) =>
//                complete(HttpResponse(StatusCodes.InternalServerError, entity = e.getMessage))
//            }
//
//          }
//        }
    }
}

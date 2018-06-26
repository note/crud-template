package tu.lambda.routes

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import cats.effect.IO
import doobie.util.transactor.Transactor.Aux
import tu.lambda.crud.entity.{Bookmark, UserId}
import tu.lambda.crud.service.BookmarkService

class BookmarkRoute(bookmarkService: BookmarkService)(implicit transactor: Aux[IO, Unit])
  extends BaseRoute {

  val route: Route =
    path("bookmarks") {
      post {
        entity(as[Bookmark]) { bookmark =>
          onSuccess(bookmarkService.save(bookmark, UserId(UUID.randomUUID())).exec) { savedBookmark =>
            complete(StatusCodes.Created -> savedBookmark)
          }
        }
      }
//      ~
//        get {
//          bookmarkService.getByUserId()
//        }
    }
}

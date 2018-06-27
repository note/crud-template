package tu.lambda.routes

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Route
import tu.lambda.crud.entity.Bookmark
import tu.lambda.crud.service.BookmarkService
import tu.lambda.crud.service.impl.{AppContext, NotGranted}

import scala.util.Try

class BookmarkRoute(bookmarkService: BookmarkService)(implicit ctx: AppContext)
  extends BaseRoute {

  val route: Route =
    path("bookmarks") {
      post {
        optionalHeaderValueByName("Authorization") {
          case Some(tokenStr) =>
            Try(UUID.fromString(tokenStr)).toOption match {
              case Some(token) =>
                entity(as[Bookmark]) { bookmark =>
                  onSuccess(bookmarkService.save(bookmark, token).exec) {
                    case Right(savedBookmark) =>
                      complete(StatusCodes.Created -> savedBookmark)
                    case Left(NotGranted) =>
                      println("bazinga 1")
                      complete(StatusCodes.Unauthorized)
                  }
                }
              case None =>
                println("bazinga 2")
                complete(StatusCodes.Unauthorized)
            }
          case None =>
            println("bazinga 3")
            complete(StatusCodes.Unauthorized)
        }

      }
//      ~
//        get {
//          bookmarkService.getByUserId()
//        }
    }
}

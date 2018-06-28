package tu.lambda.routes

import java.util.UUID

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.{Directive, Directive1, Route}
import tu.lambda.crud.entity.Bookmark
import tu.lambda.crud.service.BookmarkService
import tu.lambda.crud.service.impl.{AppContext, NotGranted}

import scala.util.Try

class BookmarkRoute(bookmarkService: BookmarkService)(implicit ctx: AppContext)
  extends BaseRoute {

  val route: Route =
    path("bookmarks") {
      post {
        authToken("Authorization") { token =>
          entity(as[Bookmark]) { bookmark =>
            onSuccess(bookmarkService.save(bookmark, token).exec) {
              case Right(savedBookmark) =>
                complete(StatusCodes.Created -> savedBookmark)
              case Left(NotGranted) =>
                println("bazinga 1")
                complete(StatusCodes.Unauthorized)
            }
          }
        }
      } ~ get {
        authToken("Authorization") { token =>
          onSuccess(bookmarkService.getByUserId(token).exec) {
            case Right(bookmarks) =>
              complete(StatusCodes.OK -> bookmarks)
            case Left(NotGranted) =>
              complete(StatusCodes.Unauthorized)
          }
        }
      }
    }

  def authToken(headerName: String): Directive[Tuple1[UUID]] = {
    val tmp: Directive1[Option[String]] = optionalHeaderValueByName(headerName)
    tmp.flatMap { tokenOpt =>
      tokenOpt.flatMap[UUID](in => Try(UUID.fromString(in)).toOption) match {
        case Some(tokenUuid) =>
          provide(tokenUuid)
        case None =>
          complete(StatusCodes.Unauthorized)
      }
    }
  }
}

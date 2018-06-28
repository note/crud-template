package tu.lambda.crud.service

import java.util.UUID

import cats.data.{EitherT, Kleisli}
import cats.effect.IO
import tu.lambda.crud.AppContext
import tu.lambda.crud.entity.{Bookmark, SavedBookmark}
import tu.lambda.crud.service.impl.BookmarkError

trait BookmarkService {
  def save(bookmark: Bookmark, token: UUID): Kleisli[EitherT[IO, BookmarkError, ?], AppContext, SavedBookmark]
  // TODO: rename it
  def getByToken(token: UUID): Kleisli[EitherT[IO, BookmarkError, ?], AppContext, List[SavedBookmark]]
}

object BookmarkService {
  sealed trait BookmarkSaveFailure {
    def message: String
  }
}
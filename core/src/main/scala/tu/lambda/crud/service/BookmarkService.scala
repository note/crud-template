package tu.lambda.crud.service

import java.util.UUID

import cats.data.{EitherT, Kleisli}
import cats.effect.IO
import tu.lambda.crud.entity.{Bookmark, SavedBookmark}
import tu.lambda.crud.service.impl.{AppContext, BookmarkError}

trait BookmarkService {
  def save(bookmark: Bookmark, token: UUID): Kleisli[EitherT[IO, BookmarkError, ?], AppContext, SavedBookmark]
  def getByUserId(token: UUID): Kleisli[EitherT[IO, BookmarkError, ?], AppContext, List[SavedBookmark]]
}

object BookmarkService {
  sealed trait BookmarkSaveFailure {
    def message: String
  }
}

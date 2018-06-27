package tu.lambda.crud.service

import java.sql.Connection
import java.util.UUID

import cats.data.{EitherT, Kleisli}
import cats.effect.IO
import tu.lambda.crud.entity.{Bookmark, SavedBookmark, UserId}
import tu.lambda.crud.service.impl.{AppContext, BookmarkError}

trait BookmarkService {
  def save(bookmark: Bookmark, token: UUID): Kleisli[EitherT[IO, BookmarkError, ?], AppContext, SavedBookmark]
  def getByUserId(userId: UserId): Kleisli[IO, Connection, List[Bookmark]]
}

object BookmarkService {
  sealed trait BookmarkSaveFailure {
    def message: String
  }
}

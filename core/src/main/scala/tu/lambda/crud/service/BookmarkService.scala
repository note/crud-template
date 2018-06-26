package tu.lambda.crud.service

import java.sql.Connection

import cats.data.Kleisli
import cats.effect.IO
import tu.lambda.crud.entity.{Bookmark, SavedBookmark, UserId}

trait BookmarkService {
  def save(bookmark: Bookmark, userId: UserId): Kleisli[IO, Connection, SavedBookmark]
  def getByUserId(userId: UserId): Kleisli[IO, Connection, List[Bookmark]]
}

object BookmarkService {
  sealed trait BookmarkSaveFailure {
    def message: String
  }
}

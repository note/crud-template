package tu.lambda.crud.service.impl

import java.sql.Connection

import cats.data.Kleisli
import cats.effect.IO
import doobie.KleisliInterpreter
import tu.lambda.crud.dao.{BookmarkDao, UUIDGenerator}
import tu.lambda.crud.entity.{Bookmark, SavedBookmark, UserId}

object DbBookmarkService {
  // TODO: extract it somewhere else?
  val interpreter = KleisliInterpreter[IO].ConnectionInterpreter

  def save(dao: BookmarkDao, uuidGen: UUIDGenerator)(bookmark: Bookmark, userId: UserId): Kleisli[IO, Connection, SavedBookmark] =
    dao.saveBookmark(bookmark, userId)(uuidGen)
      .map(id => SavedBookmark.fromBookmark(id, bookmark))
      .foldMap[Kleisli[IO, Connection, ?]](interpreter)

  def getByUserId(dao: BookmarkDao)(userId: UserId): Kleisli[IO, Connection, List[Bookmark]] =
    dao.getBookmarksByUserId(userId)
      .foldMap[Kleisli[IO, Connection, ?]](interpreter)

}

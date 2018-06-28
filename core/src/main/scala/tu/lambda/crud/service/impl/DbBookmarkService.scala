package tu.lambda.crud.service.impl

import java.util.UUID

import cats.data.{EitherT, Kleisli}
import cats.effect.IO
import cats.implicits._
import doobie.free.connection.ConnectionIO
import tu.lambda.crud.AppContext
import tu.lambda.crud.aerospike.{AerospikeClientBase, UserSessionRepo}
import tu.lambda.crud.dao.BookmarkDao
import tu.lambda.crud.entity.{Bookmark, SavedBookmark}
import tu.lambda.crud.service.BookmarkService
import tu.lambda.crud.utils.UUIDGenerator
import tu.lambda.crud.db._

class DbBookmarkService(dao: BookmarkDao, sessionRepo: UserSessionRepo)
                       (implicit uuidGen: UUIDGenerator)
    extends BookmarkService {

  // TODO: change token type to somehthing more meaningful
  def save(bookmark: Bookmark, token: UUID) = {
    for {
      sess <- aeroStack(sessionRepo.read(token))
      bookmarkId <- stacked(dao.saveBookmark(bookmark, sess.userId))
    } yield SavedBookmark.fromBookmark(bookmarkId, sess.userId, bookmark)
  }


  def getByToken(token: UUID) = {
    for {
      sess <- aeroStack(sessionRepo.read(token))
      bookmarks <- stacked(dao.getBookmarksByUserId(sess.userId))
    } yield bookmarks
  }

  private def aeroStack[T](in: Kleisli[IO, AerospikeClientBase, Option[T]]) = {
    in.local[AppContext](_.aerospikeClient).map(_.toRight[BookmarkError](NotGranted)).mapF [EitherT[IO, BookmarkError, ?], T](EitherT.apply)
  }

  private def stacked[T](program: ConnectionIO[T]): Kleisli[EitherT[IO, BookmarkError, ?], AppContext, T] =
    program.interpret.local[AppContext](_.dbConnection).map(_.asRight[BookmarkError]).mapF[EitherT[IO, BookmarkError, ?], T](EitherT.apply)

}

sealed trait BookmarkError

final case object NotGranted extends BookmarkError

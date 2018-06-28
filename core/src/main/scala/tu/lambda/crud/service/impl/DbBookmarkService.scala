package tu.lambda.crud.service.impl

import java.sql.Connection
import java.util.UUID

import cats.data.{EitherT, Kleisli}
import cats.effect.IO
import cats.implicits._
import doobie.KleisliInterpreter
import doobie.free.connection.ConnectionIO
import doobie.util.transactor.Transactor
import tu.lambda.crud.aerospike.{AerospikeClientBase, UserSessionRepo}
import tu.lambda.crud.dao.{BookmarkDao, UUIDGenerator}
import tu.lambda.crud.entity.{Bookmark, SavedBookmark}

final case class AppContext(transactor: Transactor[IO], aerospikeClient: AerospikeClientBase) {
  // TODO: it's bad
  def dbConnection: Connection = transactor.connect(transactor.kernel).unsafeRunSync()

}

class DbBookmarkService(dao: BookmarkDao, sessionRepo: UserSessionRepo)(implicit uuidGen: UUIDGenerator) {
  // TODO: extract it somewhere else?
  val interpreter = KleisliInterpreter[IO].ConnectionInterpreter

  // TODO: change token type to somehthing more meaningful
  def save(bookmark: Bookmark, token: UUID): Kleisli[EitherT[IO, BookmarkError, ?], AppContext, SavedBookmark] = {
    for {
      sess <- aeroStack(sessionRepo.read(token))
      bookmarkId <- stacked(dao.saveBookmark(bookmark, sess.userId))
    } yield SavedBookmark.fromBookmark(bookmarkId, sess.userId, bookmark)
  }


  def getByUserId(token: UUID): Kleisli[EitherT[IO, BookmarkError, ?], AppContext, List[SavedBookmark]] = {
    for {
      sess <- aeroStack(sessionRepo.read(token))
      bookmarks <- stacked(dao.getBookmarksByUserId(sess.userId))
    } yield bookmarks
  }

  def aeroStack[T](in: Kleisli[IO, AerospikeClientBase, Option[T]]) = {
    in.local[AppContext](_.aerospikeClient).map(_.toRight[BookmarkError](NotGranted)).mapF [EitherT[IO, BookmarkError, ?], T](EitherT.apply)
  }

  def stacked[T](in: ConnectionIO[T]): Kleisli[EitherT[IO, BookmarkError, ?], AppContext, T] =
    in.foldMap[Kleisli[IO, Connection, ?]](interpreter).local[AppContext](_.dbConnection).map(_.asRight[BookmarkError]).mapF[EitherT[IO, BookmarkError, ?], T](EitherT.apply)


}

sealed trait BookmarkError

final case object NotGranted extends BookmarkError
package tu.lambda.crud.service.impl

import java.sql.Connection
import java.util.UUID

import cats.implicits._
import cats.data.{EitherT, Kleisli}
import cats.effect.IO
import doobie.KleisliInterpreter
import tu.lambda.crud.aerospike.{AerospikeClient, UserSession, UserSessionRepo}
import tu.lambda.crud.dao.{BookmarkDao, UUIDGenerator}
import tu.lambda.crud.entity.{Bookmark, BookmarkId, SavedBookmark, UserId}

final case class AppContext(dbConnection: Connection, aerospikeClient: AerospikeClient)

class DbBookmarkService(dao: BookmarkDao, sessionRepo: UserSessionRepo)(implicit uuidGen: UUIDGenerator) {
  // TODO: extract it somewhere else?
  val interpreter = KleisliInterpreter[IO].ConnectionInterpreter

  // TODO: change token type to somehthing more meaningful
  def save(bookmark: Bookmark, token: UUID): Kleisli[EitherT[IO, BookmarkError, ?], AppContext, SavedBookmark] = {
//    val tmp: Kleisli[IO, Connection, SavedBookmark] = dao.saveBookmark(bookmark, userId)(uuidGen)
//      .map(id => SavedBookmark.fromBookmark(id, userId, bookmark))
//      .foldMap[Kleisli[IO, Connection, ?]](interpreter)


    val t1 = sessionRepo.read(token).local[AppContext](_.aerospikeClient).map(_.toRight[BookmarkError](NotGranted)).mapF [EitherT[IO, BookmarkError, ?], UserSession] { io =>
      val tmp: EitherT[IO, BookmarkError, UserSession] = EitherT.apply(io)
      tmp
    }

    for {
      sess <- t1
      bookmarkId <- dao.saveBookmark(bookmark, sess.userId).foldMap[Kleisli[IO, Connection, ?]](interpreter).local[AppContext](_.dbConnection).map(_.asRight[BookmarkError]).mapF[EitherT[IO, BookmarkError, ?], BookmarkId](EitherT.apply)
    } yield SavedBookmark.fromBookmark(bookmarkId, sess.userId, bookmark)

  }


  def getByUserId(userId: UserId): Kleisli[IO, Connection, List[Bookmark]] =
    dao.getBookmarksByUserId(userId)
      .foldMap[Kleisli[IO, Connection, ?]](interpreter)

}

sealed trait BookmarkError

final case object NotGranted extends BookmarkError
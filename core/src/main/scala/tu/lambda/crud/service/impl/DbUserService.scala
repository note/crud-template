package tu.lambda.crud.service.impl

import java.sql.Connection

import cats.Applicative
import cats.data.Validated.{invalidNel, valid, _}
import cats.data.{Kleisli, NonEmptyList, ValidatedNel}
import cats.effect.IO
import cats.implicits._
import doobie._
import tu.lambda.crud.dao.{UUIDGenerator, UserDao}
import tu.lambda.crud.entity.{SavedUser, User, UserId}
import tu.lambda.crud.service.UserService.UserSaveFailure
import tu.lambda.crud.service.UserService.UserSaveFailure._

object DbUserService {
  // TODO: extract it somewhere else?
  val interpreter = KleisliInterpreter[IO].ConnectionInterpreter

  def save(dao: UserDao, uuidGen: UUIDGenerator)(user: User): Kleisli[IO, Connection, Either[NonEmptyList[UserSaveFailure], SavedUser]] = {
    validate(user).toEither match {
      case Right(validUser) =>
        dao.saveUser(validUser)(uuidGen)
          .foldMap[Kleisli[IO, Connection, ?]](interpreter)
          .map(id => SavedUser.fromUser(UserId(id), user).asRight[NonEmptyList[UserSaveFailure]])
      case Left(errors) =>
        Kleisli.liftF(IO.pure(errors.asLeft[SavedUser]))
    }
  }

  def getByCredentials(dao: UserDao)(email: String, password: String): Kleisli[IO, Connection, Option[SavedUser]] = {
    dao
      .getUserByCredentials(email, password)
      .foldMap[Kleisli[IO, Connection, ?]](interpreter)
  }

  private def validate(user: User): ValidatedNel[UserSaveFailure, User] =
    Applicative[ValidatedNel[UserSaveFailure, ?]].map2(validateEmail(user.email), validatePassword(user.password))((_, _) => user)

  private val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".r

  private def validateEmail(email: String): ValidatedNel[UserSaveFailure, Unit] =
    (emailPattern findFirstIn email) match {
      case Some(_)  => valid(())
      case None     => invalidNel(IncorrectEmail)
    }

  private def validatePassword(password: String): ValidatedNel[UserSaveFailure, Unit] =
    Applicative[ValidatedNel[UserSaveFailure, ?]].map2(validatePasswordFormat(password), validatePasswordLength(password))((_, _) => ())

  private def validatePasswordFormat(password: String): ValidatedNel[UserSaveFailure, Unit] =
    if (password.exists(_.isWhitespace)) {
      invalidNel(PasswordContainsWhiteSpace)
    } else {
      valid(())
    }

  private def validatePasswordLength(password: String): ValidatedNel[UserSaveFailure, Unit] =
    if (password.size >= 6) {
      valid(())
    } else {
      invalidNel(PasswordTooShort)
    }

}

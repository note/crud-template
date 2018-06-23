package tu.lambda.crud.service.impl

import java.sql.Connection

import cats.Applicative
import cats.data.Validated.{invalidNel, valid, _}
import cats.data.{Kleisli, NonEmptyList, ValidatedNel}
import cats.effect.IO
import cats.implicits._
import doobie._
import monix.eval.Task
import tu.lambda.crud.dao.{UUIDGenerator, UserDao}
import tu.lambda.crud.entity.{SavedUser, User, UserId}
import tu.lambda.crud.service.UserService
import tu.lambda.crud.service.UserService.UserSaveFailure
import tu.lambda.crud.service.UserService.UserSaveFailure._

class DbUserService()(implicit uuidGen: UUIDGenerator) extends UserService {
  val interpreter = KleisliInterpreter[IO].ConnectionInterpreter

  override def save(user: User): Kleisli[IO, Connection, Either[NonEmptyList[UserSaveFailure], SavedUser]] = {
//    val a = UserDao.saveUser(user)
//    a.foldMap(interpreter).run

    validate(user).toEither match {
      case Right(validUser) =>
        UserDao.saveUser(validUser)
          .foldMap[Kleisli[IO, Connection, ?]](interpreter)
          .map(id => SavedUser.fromUser(UserId(id), user).asRight[NonEmptyList[UserSaveFailure]])

      case Left(errors) =>
        Kleisli.liftF(IO.pure(errors.asLeft[SavedUser]))
    }
  }

  def getByCredentials(email: String, password: String): Task[Option[SavedUser]] = ???

  private def validate(user: User): ValidatedNel[UserSaveFailure, User] =
    Applicative[ValidatedNel[UserSaveFailure, ?]].map2(validateEmail(user.email), validatePassword(user.password))((_, _) => user)

  private val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".r

  private def validateEmail(email: String): ValidatedNel[UserSaveFailure, Unit] =
    (emailPattern findFirstIn email) match {
      case Some(_) => valid(())
      case None => invalidNel(IncorrectEmail)
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

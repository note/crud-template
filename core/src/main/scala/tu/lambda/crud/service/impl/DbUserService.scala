package tu.lambda.crud.service.impl

import cats.data.Validated.{invalidNel, valid, _}
import cats.data.{Kleisli, OptionT, ValidatedNel}
import cats.effect.IO
import cats.implicits._
import tu.lambda.crud.AppContext
import tu.lambda.crud.aerospike.{UserSession, UserSessionRepo}
import tu.lambda.crud.dao.UserDao
import tu.lambda.crud.entity.{SavedUser, User}
import tu.lambda.crud.service.UserService
import tu.lambda.crud.service.UserService.UserSaveFailure
import tu.lambda.crud.service.UserService.UserSaveFailure._
import tu.lambda.crud.utils.UUIDGenerator
import tu.lambda.crud.db._

import scala.concurrent.duration._

class DbUserService(dao: UserDao, sessionRepo: UserSessionRepo)
                   (implicit uuidGen: UUIDGenerator) extends UserService {

  def save(user: User) = {
    validate(user).toEither match {
      case Right(validUser) =>
        dao
          .saveUser(validUser)(uuidGen)
          .interpret
          .map(id => SavedUser.fromUser(id, user).asRight)
      case Left(errors) =>
        Kleisli.liftF(IO.pure(errors.asLeft[SavedUser]))
    }
  }

  def login(expiration: Duration)(email: String, password: String) = {
    for {
      user    <- getByCredentials(email, password)
      session = UserSession(user.id, uuidGen.generate())
      // TODO: expiration - from config?
      _       <- sessionRepo.insert(expiration)(session).local[AppContext](_.aerospikeClient)
                  .mapF [OptionT[IO, ?], Unit](t => OptionT.apply(t.map(_.some)))
    } yield session
  }.mapF(_.value)

  private def getByCredentials(email: String, password: String) =
    dao
      .getUserByCredentials(email, password)
      .interpret
      .local[AppContext](_.dbConnection)
      .mapF[OptionT[IO, ?], SavedUser](OptionT.apply)

  private def validate(user: User): ValidatedNel[UserSaveFailure, User] =
    (validateEmail(user.email), validatePassword(user.password)).mapN((_, _) => user)

  private val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".r

  private def validateEmail(email: String): ValidatedNel[UserSaveFailure, Unit] =
    (emailPattern findFirstIn email) match {
      case Some(_)  => valid(())
      case None     => invalidNel(IncorrectEmail)
    }

  private def validatePassword(password: String): ValidatedNel[UserSaveFailure, Unit] =
    (validatePasswordFormat(password), validatePasswordLength(password)).mapN((_, _) => ())

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

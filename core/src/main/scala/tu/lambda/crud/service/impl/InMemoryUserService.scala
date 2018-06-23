package tu.lambda.crud.service.impl

import java.util.UUID

import cats.Applicative
import cats.data.Validated.{ invalidNel, valid, _ }
import cats.data.ValidatedNel
import monix.eval.Task
import tu.lambda.crud.entity.{ SavedUser, User, UserId }
import tu.lambda.crud.service.UserService
import tu.lambda.crud.service.UserService._

import scala.collection.mutable

class InMemoryUserService extends UserService {
  private val users = mutable.Map.empty[UserId, SavedUser]

  override def save(user: User): Task[ValidatedNel[UserSaveError, SavedUser]] = {
    Task.now(validate(user).map { validUser =>
      val userId = UserId(UUID.randomUUID())
      val savedUser = SavedUser.fromUser(userId, validUser)
      users += (userId -> savedUser)
      savedUser
    })
  }

  override def get(userId: UserId): Task[Option[SavedUser]] = {
    Task.now(users.get(userId))
  }

  private def validate(user: User): ValidatedNel[UserSaveError, User] =
    Applicative[ValidatedNel[UserSaveError, ?]].map2(validateEmail(user.email), validatePassword(user.password))((_, _) => user)

  private val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".r

  private def validateEmail(email: String): ValidatedNel[UserSaveError, Unit] =
    (emailPattern findFirstIn email) match {
      case Some(_) => valid(())
      case None => invalidNel(IncorrectEmail)
    }

  private def validatePassword(password: String): ValidatedNel[UserSaveError, Unit] =
    Applicative[ValidatedNel[UserSaveError, ?]].map2(validatePasswordFormat(password), validatePasswordLength(password))((_, _) => ())

  private def validatePasswordFormat(password: String): ValidatedNel[UserSaveError, Unit] =
    if (password.exists(_.isWhitespace)) {
      invalidNel(PasswordContainsWhiteSpace)
    } else {
      valid(())
    }

  private def validatePasswordLength(password: String): ValidatedNel[UserSaveError, Unit] =
    if (password.size >= 6) {
      valid(())
    } else {
      invalidNel(PasswordTooShort)
    }

}

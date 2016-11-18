package net.michalsitko.crud.service

import java.util.UUID

import cats.data.ValidatedNel
import cats.data.Validated.{ valid, invalidNel }
import cats.implicits._
import net.michalsitko.crud.entity.{ SavedUser, User, UserId }

import scala.collection.mutable
import scala.concurrent.Future

sealed trait UserSaveError {
  def message: String
}
case object IncorrectEmail extends UserSaveError {
  override def message: String = "Incorrect email"
}

case object PasswordTooShort extends UserSaveError {
  override def message: String = "Password too short"
}

case object PasswordContainsWhiteSpace extends UserSaveError {
  override def message: String = "Password too short"
}

trait UserService {
  def save(user: User): Future[ValidatedNel[UserSaveError, SavedUser]]
  def get(userId: UserId): Future[Option[SavedUser]]
}

class InMemoryUserService extends UserService {
  private var users = mutable.Map.empty[UserId, SavedUser]

  override def save(user: User): Future[ValidatedNel[UserSaveError, SavedUser]] = {
    Future.successful(validate(user).map { validUser =>
      val userId = UserId(UUID.randomUUID())
      val savedUser = SavedUser.fromUser(userId, validUser)
      users += (userId -> savedUser)
      savedUser
    })
  }

  override def get(userId: UserId): Future[Option[SavedUser]] = {
    Future.successful(users.get(userId))
  }

  private def validate(user: User): ValidatedNel[UserSaveError, User] =
    (validateEmail(user.email) |@| validatePassword(user.password)).map { (_, _) => user }

  private val emailPattern = "^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,6}$".r

  private def validateEmail(email: String): ValidatedNel[UserSaveError, Unit] =
    (emailPattern findFirstIn email) match {
      case Some(_) => valid(())
      case None => invalidNel(IncorrectEmail)
    }

  private def validatePassword(password: String): ValidatedNel[UserSaveError, Unit] =
    (validatePasswordFormat(password) |@| (validatePasswordLength(password))).map { (_, _) => () }

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

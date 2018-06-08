package net.michalsitko.crud.service

import cats.data.ValidatedNel
import monix.eval.Task
import net.michalsitko.crud.entity.{ SavedUser, User, UserId }

trait UserService {
  import UserService._

  def save(user: User): Task[ValidatedNel[UserSaveError, SavedUser]]
  def get(userId: UserId): Task[Option[SavedUser]]
}

object UserService {
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
}
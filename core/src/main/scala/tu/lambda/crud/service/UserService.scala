package tu.lambda.crud.service

import java.sql.Connection

import cats.data.{Kleisli, NonEmptyList}
import cats.effect.IO
import tu.lambda.crud.entity.{SavedUser, User}

trait UserService {
  import UserService._

  def save(user: User): Kleisli[IO, Connection, Either[NonEmptyList[UserSaveFailure], SavedUser]]
  def getByCredentials(email: String, password: String): Kleisli[IO, Connection, Option[SavedUser]]
}

object UserService {
  sealed trait UserSaveFailure {
    def message: String
  }

  object UserSaveFailure {
    case object IncorrectEmail extends UserSaveFailure {
      override def message: String = "Incorrect email"
    }

    // TODO: add this one!
    case object EmailAlreadyExists extends UserSaveFailure {
      override def message: String = "Email already exists"
    }

    case object PasswordTooShort extends UserSaveFailure {
      override def message: String = "Password too short"
    }

    case object PasswordContainsWhiteSpace extends UserSaveFailure {
      override def message: String = "Password contains white space"
    }
  }

}
package tu.lambda.crud.service

import java.sql.Connection

import cats.data.{Kleisli, ValidatedNel}
import cats.effect.IO
import tu.lambda.crud.AppContext
import tu.lambda.crud.aerospike.UserSession
import tu.lambda.crud.entity.{SavedUser, User}

import scala.concurrent.duration.Duration

trait UserService {
  import UserService._

  def save(user: User): Kleisli[IO, Connection, ValidatedNel[UserSaveFailure, SavedUser]]
  def login(expiration: Duration)(email: String, password: String): Kleisli[IO, AppContext, Option[UserSession]]
}

object UserService {
  sealed trait UserSaveFailure {
    def message: String
  }

  object UserSaveFailure {
    case object IncorrectEmail extends UserSaveFailure {
      override val message: String = "Incorrect email"
    }

    case object EmailAlreadyExists extends UserSaveFailure {
      override val message: String = "Email already exists"
    }

    case object PasswordTooShort extends UserSaveFailure {
      override val message: String = "Password too short"
    }

    case object PasswordContainsWhiteSpace extends UserSaveFailure {
      override val message: String = "Password contains white space"
    }
  }

}
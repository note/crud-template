package tu.lambda.crud.aerospike

import cats.data.Kleisli
import cats.effect.IO
import cats.implicits._
import tu.lambda.crud.entity.{Token, UserId}

import scala.concurrent.duration.Duration

final case class UserSession (userId: UserId, token: Token)

trait UserSessionRepo {
  def insert(expiration: Duration)(session: UserSession): Kleisli[IO, AerospikeClientBase, Unit]
  def read(token: Token): Kleisli[IO, AerospikeClientBase, Option[UserSession]]
}

object UserSessionRepo extends UserSessionRepo {
  private val UserSessionSetName = "sessions"
  private val UserSessionBinName = "sessionBin"

  // TODO: example of currying
  private val USKey = Key.curried(UserSessionSetName)
  private val USBin = Bin.curried(UserSessionBinName)

  def insert(expiration: Duration)(session: UserSession): Kleisli[IO, AerospikeClientBase, Unit] =
    Kleisli { client =>
      implicit val policy = WritePolicy(expiration)

      val key = USKey(session.token.toString)
      val bin = USBin(session.userId.toString)
      client.insert(key, bin)
    }

  def read(token: Token): Kleisli[IO, AerospikeClientBase, Option[UserSession]] =
    Kleisli { client =>
      client.read(USKey(token.toString), UserSessionBinName).flatMap {
        case Some(userIdString) =>
          val userSession = UserId.fromString(userIdString).map { userId =>
            UserSession(userId, token)
          }.toEither.right.map(_.some)

          IO.fromEither(userSession)
        case None =>
          IO.pure(Option.empty[UserSession])
      }
    }
}

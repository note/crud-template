package tu.lambda.crud.aerospike

import java.util.UUID

import cats.data.Kleisli
import cats.effect.IO
import cats.implicits._
import tu.lambda.crud.entity.UserId

import scala.concurrent.duration.Duration

// in real world application UUID might not be the best choice...
final case class UserSession (userId: UserId, token: UUID)

trait UserSessionRepo {
  def insert(expiration: Duration)(session: UserSession): Kleisli[IO, AerospikeClientBase, Unit]
  def read(token: UUID): Kleisli[IO, AerospikeClientBase, Option[UserSession]]
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
      val bin = USBin(session.userId.id.toString)
      client.insert(key, bin)
    }

  def read(token: UUID): Kleisli[IO, AerospikeClientBase, Option[UserSession]] =
    Kleisli { client =>
      client.read(USKey(token.toString), UserSessionBinName).flatMap {
        case Some(userIdString) =>
          println("bazinga 100")
          val userSession = UserId.fromString(userIdString).map { userId =>
            UserSession(userId, token)
          }.toEither.right.map(_.some)

          IO.fromEither(userSession)
        case None =>
          println("bazinga 101")
          IO.pure(Option.empty[UserSession])
      }
    }
}

package net.michalsitko.format

import java.util.UUID

import cats.syntax.either._
import io.circe.{ Decoder, Encoder }
import net.michalsitko.crud.entity.{ SavedUser, User, UserId }
import io.circe.generic.semiauto._

object Formats {
  // Leave it unimplemented as an Excercise:
  implicit val userDecoder: Decoder[User] = deriveDecoder[User]

  implicit val saveUserEncoder: Encoder[SavedUser] = deriveEncoder[SavedUser]

  implicit val encodeUserId: Encoder[UserId] =
    Encoder.encodeString.contramap[UserId](_.id.toString)

  implicit val decodeUserId: Decoder[UserId] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(UserId(UUID.fromString(str))).leftMap(t => "UserId")
  }

}

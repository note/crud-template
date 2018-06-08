package net.michalsitko.format

import java.util.UUID

import cats.syntax.either._
import io.circe.{ Decoder, Encoder }
import net.michalsitko.crud.entity.{ SavedUser, User, UserId }
import io.circe.generic.semiauto._

object Formats {
  // it does exactly the same thing as the version below
  //  implicit val encodeUserId: Encoder[UserId] = new Encoder[UserId] {
  //    final def apply(userId: UserId): Json = Json.fromString(userId.id.toString)
  //  }

  implicit val encodeUserId: Encoder[UserId] =
    Encoder.encodeString.contramap[UserId](_.id.toString)

  implicit val decodeUserId: Decoder[UserId] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(UserId(UUID.fromString(str))).leftMap(t => "UserId")
  }

  implicit val userDecoder: Decoder[User] = deriveDecoder[User]

  implicit val saveUserEncoder: Encoder[SavedUser] = deriveEncoder[SavedUser]

}

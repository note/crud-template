package net.michalsitko.format

import java.util.UUID

import cats.syntax.either._
import io.circe.{ Decoder, Encoder, Json }
import net.michalsitko.crud.entity.UserId

object Formats {
  implicit val encodeUserId: Encoder[UserId] = new Encoder[UserId] {
    final def apply(userId: UserId): Json = Json.fromString(userId.id.toString)
  }

  implicit val decodeUserId: Decoder[UserId] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(UserId(UUID.fromString(str))).leftMap(t => "UserId")
  }

}

package tu.lambda.format

import java.util.UUID

import cats.syntax.either._
import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import tu.lambda.crud.entity.{SavedUser, User, UserId}
import tu.lambda.entity.Credentials

trait JsonFormats {
  // Leave it unimplemented as an TalkExample:
  implicit val userDecoder: Decoder[User] = deriveDecoder[User]

  implicit val saveUserEncoder: Encoder[SavedUser] = deriveEncoder[SavedUser]

  implicit val userIdEncoder: Encoder[UserId] =
    Encoder.encodeString.contramap[UserId](_.id.toString)

  implicit val userIdDecoder: Decoder[UserId] = Decoder.decodeString.emap { str =>
    Either.catchNonFatal(UserId(UUID.fromString(str))).leftMap(t => "UserId")
  }

  implicit val credentialsDecoder = deriveDecoder[Credentials]

}

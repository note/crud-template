package tu.lambda.format

import java.net.URL

import io.circe.{Decoder, Encoder}
import io.circe.generic.semiauto._
import tu.lambda.crud.aerospike.UserSession
import tu.lambda.crud.entity._
import tu.lambda.entity.Credentials

import scala.util.Try

trait JsonFormats {
//  implicit val userDecoder: Decoder[User] = ???

  implicit val credentialsDecoder = deriveDecoder[Credentials]

  implicit val urlDecoder: Decoder[URL] = Decoder.decodeString
    .emapTry(in => Try(new URL(in)))
    .withErrorMessage("Incorrect URL format")

  implicit val urlEncoder: Encoder[URL] = Encoder.encodeString.contramap[URL](_.toString)

  implicit val bookmarkIdEncoder: Encoder[BookmarkId] =
    Encoder.encodeString.contramap[BookmarkId](_.toString)

  implicit val bookmarkDecoder      = deriveDecoder[Bookmark]
  implicit val savedBookmarkEncoder: Encoder[SavedBookmark] = ???
  implicit val tokenEncoder         = Encoder.encodeString.contramap[Token](_.toString)
  implicit val userSessionsEncoder: Encoder[UserSession] = ???

}

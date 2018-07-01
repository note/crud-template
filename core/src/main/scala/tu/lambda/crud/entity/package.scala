package tu.lambda.crud

import java.util.UUID

import shapeless.tag
import shapeless.tag.@@
import tu.lambda.crud.utils.UUIDGenerator

import scala.util.Try

trait UserIdTag
trait BookmarkIdTag
trait TokenTag

package object entity {

  type UserId = UUID @@ UserIdTag

  object UserId {
    def apply(uuid: UUID): UserId =
      tag[UserIdTag][UUID](uuid)

    def fromString(input: String): Try[UserId] = Try {
      tag[UserIdTag][UUID](UUID.fromString(input))
    }

    def generate(implicit gen: UUIDGenerator): UserId =
      UserId(gen.generate())
  }

  type BookmarkId = UUID @@ BookmarkIdTag

  object BookmarkId {
    def apply(uuid: UUID): BookmarkId =
      tag[BookmarkIdTag][UUID](uuid)

    def generate(implicit gen: UUIDGenerator): BookmarkId =
      BookmarkId(gen.generate())
  }

  // in real world application UUID might not be the best choice...
  type Token = UUID @@ TokenTag

  object Token {
    def apply(uuid: UUID): Token =
      tag[TokenTag][UUID](uuid)

    def generate(implicit gen: UUIDGenerator): Token =
      Token(gen.generate())
  }

}

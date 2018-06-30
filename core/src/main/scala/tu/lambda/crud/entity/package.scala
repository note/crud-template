package tu.lambda.crud

import java.util.UUID

import shapeless.tag
import shapeless.tag.@@

import scala.util.Try

trait UserIdTag
trait BookmarkIdTag

package object entity {

  type UserId = UUID @@ UserIdTag

  object UserId {
    def apply(uuid: UUID): UserId =
      tag[UserIdTag][UUID](uuid)

    def fromString(input: String): Try[UserId] = Try {
      tag[UserIdTag][UUID](UUID.fromString(input))
    }
  }

  type BookmarkId = UUID @@ BookmarkIdTag

  object BookmarkId {
    def apply(uuid: UUID): BookmarkId =
      tag[BookmarkIdTag][UUID](uuid)
  }

}

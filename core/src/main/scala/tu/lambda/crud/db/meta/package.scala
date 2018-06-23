package tu.lambda.crud.db

import java.util.UUID

import doobie.util.meta.Meta

package object meta {
  implicit val UUIDMeta: Meta[UUID] =
    Meta[String].xmap(UUID.fromString, _.toString)
}

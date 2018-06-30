package tu.lambda.crud.db

import java.net.URL
import java.util.UUID

import doobie.util.meta.Meta
import doobie.postgres.implicits._
import tu.lambda.crud.entity.{BookmarkId, UserId}

package object meta {
  implicit val UrlMeta: Meta[URL] =
    Meta[String].xmap(new URL(_), _.toString)

  // TODO: all those Ids metas can be generalized
  implicit val userIdMeta: Meta[UserId] =
    Meta[UUID].xmap(UserId.apply, identity)

  implicit val bookmarkIdMeta: Meta[BookmarkId] =
    Meta[UUID].xmap(BookmarkId.apply, identity)
}

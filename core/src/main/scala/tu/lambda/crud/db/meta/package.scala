package tu.lambda.crud.db

//import java.util.UUID
//
import java.net.URL

import doobie.util.meta.Meta

package object meta {
//  implicit val UUIDMeta: Meta[UUID] =
//    Meta[String].xmap(UUID.fromString, _.toString)

  implicit val UrlMeta: Meta[URL] =
    Meta[String].xmap(new URL(_), _.toString)
}

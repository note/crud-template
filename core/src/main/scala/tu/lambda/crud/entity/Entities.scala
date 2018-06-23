package tu.lambda.crud.entity

import java.net.URL
import java.util.UUID

// TODO: try to user more specific types, think about refined, password as Array of bytes?
final case class User(email: String, phone: String, password: String)

final case class UserId(id: UUID) extends AnyVal
final case class SavedUser(id: UserId, email: String, phone: String)

object SavedUser {
  def fromUser(userId: UserId, user: User): SavedUser =
    SavedUser(userId, user.email, user.phone)
}

// TalkExample: customer circe Decoder
final case class BookmarksImport(bookmarks: List[Bookmark])

final case class Bookmark(url: URL, title: String)

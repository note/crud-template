package tu.lambda.crud.entity

import java.net.URL
import java.util.UUID

import scala.util.Try

// TODO: try to user more specific types, think about refined, password as Array of bytes?
final case class User(email: String, phone: String, password: String)

final case class UserId(id: UUID) extends AnyVal

object UserId {
  def fromString(input: String): Try[UserId] = Try {
    UserId(UUID.fromString(input))
  }
}

final case class SavedUser(id: UserId, email: String, phone: String)

object SavedUser {
  def fromUser(id: UserId, user: User): SavedUser =
    SavedUser(id, user.email, user.phone)
}

// TalkExample: customer circe Decoder
final case class BookmarksImport(bookmarks: List[Bookmark])

final case class BookmarkId(id: UUID) extends AnyVal
final case class Bookmark(url: URL, description: String)

final case class SavedBookmark(id: BookmarkId, userId: UserId, url: URL, description: String)

object SavedBookmark {
  def fromBookmark(id: BookmarkId, userId: UserId, bookmark: Bookmark): SavedBookmark =
    SavedBookmark(id, userId, bookmark.url, bookmark.description)
}


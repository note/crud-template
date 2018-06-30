package tu.lambda.crud.entity

import java.net.URL

// In real world project storing password as String might not be good idea
final case class User(email: String, phone: String, password: String)

final case class SavedUser(id: UserId, email: String, phone: String)

object SavedUser {
  def fromUser(id: UserId, user: User): SavedUser =
    SavedUser(id, user.email, user.phone)
}

// TalkExample: customer circe Decoder
final case class BookmarksImport(bookmarks: List[Bookmark])

final case class Bookmark(url: URL, description: String)

final case class SavedBookmark(id: BookmarkId, userId: UserId, url: URL, description: String)

object SavedBookmark {
  def fromBookmark(id: BookmarkId, userId: UserId, bookmark: Bookmark): SavedBookmark =
    SavedBookmark(id, userId, bookmark.url, bookmark.description)
}


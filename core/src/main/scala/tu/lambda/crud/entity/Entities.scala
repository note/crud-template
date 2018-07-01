package tu.lambda.crud.entity

import java.net.URL

// case class - immutable value object:
// - it does not have lifecycle, hence immutable
// - it components define equals method - 2 different instances with the same values are
//   considered equal
final case class User(email: String, phone: String, password: String)

final case class SavedUser(id: UserId, email: String, phone: String)

/*
Naively modelled:
case class User(id: UUID, email: String, phone: String, password: String)

Now when creating a new user:

User(null, email, phone, password)

That's quite terrible as it uses null. Quoting Tony Hoare:

I call it my billion-dollar mistake. It was the invention of the null reference in 1965.
At that time, I was designing the first comprehensive type system for references
in an object oriented language (ALGOL W).
This has led to innumerable errors, vulnerabilities, and system crashes, which have
probably caused a billion dollars of pain and damage in the last forty years.

A bit better:

case class User(id: Option[UUID], email: String, phone: String, password: String)

But the essence of the problem remains - we are modelling two different entities with the same class.

This is wrong approach to data modelling. Preferable one is to:

Make illegal states unrepresentable
(Yaron Minsky)

Make implicit concepts explicit

(Eric Evans in Domain Driven Design)
 */


object SavedUser {
  def fromUser(id: UserId, user: User): SavedUser =
    SavedUser(id, user.email, user.phone)
}

final case class BookmarksImport(bookmarks: List[Bookmark])

final case class Bookmark(url: URL, description: String)

final case class SavedBookmark(id: BookmarkId, userId: UserId, url: URL, description: String)

object SavedBookmark {
  def fromBookmark(id: BookmarkId, userId: UserId, bookmark: Bookmark): SavedBookmark =
    SavedBookmark(id, userId, bookmark.url, bookmark.description)
}


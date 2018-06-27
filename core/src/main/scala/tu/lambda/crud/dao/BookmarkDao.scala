package tu.lambda.crud.dao

import doobie.ConnectionIO
import doobie.implicits._
import doobie.postgres.implicits._
import tu.lambda.crud.entity.{Bookmark, BookmarkId, SavedBookmark, UserId}
import tu.lambda.crud.db.meta._

trait BookmarkDao {
  def saveBookmark(bookmark: Bookmark, userId: UserId)(implicit uuidGen: UUIDGenerator): ConnectionIO[BookmarkId]
  def getBookmarksByUserId(userId: UserId): ConnectionIO[List[SavedBookmark]]
}

object BookmarkDao extends BookmarkDao {
  override def saveBookmark(bookmark: Bookmark, userId: UserId)(implicit uuidGen: UUIDGenerator): ConnectionIO[BookmarkId] = {
    val uuid = uuidGen.generate()

    sql"""INSERT INTO bookmarks (id, user_id, url, description)
      |     VALUES ($uuid,
      |             $userId,
      |             ${bookmark.url},
      |             ${bookmark.description})
    """.stripMargin.update.run.map(_ => BookmarkId(uuid))
  }

  override def getBookmarksByUserId(userId: UserId): ConnectionIO[List[SavedBookmark]] =
    sql"""SELECT id, user_id, url, description FROM bookmarks
         |  WHERE userId = ${userId.id}
    """.stripMargin.query[SavedBookmark].to[List]
}

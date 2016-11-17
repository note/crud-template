package net.michalsitko.crud.entity

import java.util.UUID

case class User(email: String, phone: String, password: String)

case class UserId(id: UUID) extends AnyVal
case class SavedUser(id: UserId, email: String, phone: String, password: String)

object SavedUser {
  def fromUser(userId: UserId, user: User): SavedUser =
    SavedUser(userId, user.email, user.phone, user.password)
}

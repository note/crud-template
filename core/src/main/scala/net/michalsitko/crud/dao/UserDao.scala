package net.michalsitko.crud.dao

import doobie._
import doobie.implicits._
import cats.implicits._

object UserDao {
  val program1 = 42.pure[ConnectionIO]
}

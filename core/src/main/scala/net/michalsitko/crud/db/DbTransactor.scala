package net.michalsitko.crud.db

import cats.effect.IO
import doobie._

object DbTransactor {
  def transactor = Transactor.fromDriverManager[IO](
    "org.postgresql.Driver", // driver classname
    "jdbc:postgresql:world", // connect URL (driver-specific)
    "postgres", // user
    "" // password
  )
}

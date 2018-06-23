package net.michalsitko.crud.db

import cats.effect.IO
import doobie._
import doobie.util.transactor.Transactor.Aux
import net.michalsitko.crud.config.DbConfig

object DbTransactor {
  def transactor(dbConfig: DbConfig): Aux[IO, Unit] = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver", // driver classname
    url = dbConfig.url, // connect URL (driver-specific)
    user = dbConfig.user, // user
    pass = dbConfig.password // password
  )
}

package tu.lambda.crud.db

import cats.effect.IO
import doobie.util.transactor.Transactor
import tu.lambda.crud.config.DbConfig

object DbTransactor {
  def transactor(dbConfig: DbConfig): Transactor[IO] = Transactor.fromDriverManager[IO](
    driver = "org.postgresql.Driver", // driver classname
    url = dbConfig.url, // connect URL (driver-specific)
    user = dbConfig.user, // user
    pass = dbConfig.password // password
  )
}

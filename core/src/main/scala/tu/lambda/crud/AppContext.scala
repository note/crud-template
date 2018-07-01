package tu.lambda.crud

import java.sql.Connection

import cats.effect.IO
import doobie.util.transactor.Transactor
import tu.lambda.crud.aerospike.AerospikeClientBase

final case class AppContext(transactor: Transactor[IO], aerospikeClient: AerospikeClientBase) {
  def dbConnection: Connection = transactor.connect(transactor.kernel).unsafeRunSync()
}

package tu.lambda.config

import tu.lambda.crud.config.{AerospikeConfig, DbConfig}

import scala.concurrent.duration.Duration

final case class AppConfig(
  binding: HttpConfig,
  aerospike: AerospikeConfig,
  db: DbConfig,
  tokenExpiration: Duration)

final case class HttpConfig(
  scheme: String,
  host: String,
  port: Int)

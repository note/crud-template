package tu.lambda.config

import tu.lambda.crud.config.{AerospikeConfig, DbConfig}

final case class AppConfig(
  binding: HttpConfig,
  aerospike: AerospikeConfig,
  db: DbConfig)

final case class HttpConfig(
  scheme: String,
  host: String,
  port: Int)

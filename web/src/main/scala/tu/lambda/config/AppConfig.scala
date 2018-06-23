package tu.lambda.config

import tu.lambda.crud.config.DbConfig

final case class AppConfig(
  binding: HttpConfig,
  redis: RedisConfig,
  db: DbConfig)

final case class HttpConfig(
  scheme: String,
  host: String,
  port: Int)

final case class RedisConfig(host: String, port: Int)
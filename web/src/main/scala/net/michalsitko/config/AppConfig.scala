package net.michalsitko.config

case class AppConfig(binding: HttpConfig)

case class HttpConfig(
  scheme: String,
  host: String,
  port: Int
)

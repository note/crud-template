package tu.lambda.crud.config

final case class DbConfig(url: String, user: String, password: String)

final case class AerospikeConfig(host: String, port: Int, namespace: String)

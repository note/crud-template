package tu.lambda.crud.aerospike

import cats.effect.IO
import com.aerospike.{client => javaClient}
import tu.lambda.crud.config.AerospikeConfig

import scala.concurrent.duration.Duration

class AerospikeClient (config: AerospikeConfig) {
  private val client = new javaClient.AerospikeClient(config.host, config.port)

  def insert(key: Key, bin: Bin)(implicit policy: WritePolicy): IO[Unit] = IO {
    client.put(policy.asJava, key.asJava(config.namespace), bin.asJava)
  }

  def read(key: Key, binName: String): IO[Option[String]] = IO {
    for {
      record <- Option(client.get(null, key.asJava(config.namespace), binName))
      value <- Option(record.getString(binName))
    } yield value
  }
}

final case class Key(setName: String, key: String) {
  def asJava(namespace: String): javaClient.Key =
    new javaClient.Key(namespace, setName, key)
}

final case class Bin(name: String, value: String) {
  def asJava: javaClient.Bin =
    new javaClient.Bin(name, value)
}

final case class WritePolicy(expiration: Duration) {
  def asJava: javaClient.policy.WritePolicy = {
    val p = new javaClient.policy.WritePolicy()
    p.expiration = expiration.toSeconds.toInt
    p
  }

}

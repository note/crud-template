package tu.lambda.crud.utils

import java.util.UUID

trait UUIDGenerator {
  def generate(): UUID
}

object UUIDGenerator {
  val default: UUIDGenerator = () => UUID.randomUUID()
}

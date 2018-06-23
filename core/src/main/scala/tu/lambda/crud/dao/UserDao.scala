package tu.lambda.crud.dao

import java.util.UUID

import tu.lambda.crud.entity.{SavedUser, User}
import doobie.implicits._
import doobie.postgres.implicits._

// TODO: move it somewhere else
trait UUIDGenerator {
  def generate(): UUID
}

object UUIDGenerator {
  val default: UUIDGenerator = () => UUID.randomUUID()
}

trait UserDao {
  def saveUser(user: User)(implicit uuidGen: UUIDGenerator): doobie.ConnectionIO[UUID]
  def getUserByCredentials(email: String, password: String): doobie.ConnectionIO[Option[SavedUser]]
}

object UserDao extends UserDao {
  def saveUser(user: User)(implicit uuidGen: UUIDGenerator): doobie.ConnectionIO[UUID] = {
    val uuid = uuidGen.generate()

    sql"""|INSERT INTO users (id, email, phone, password)
          |  VALUES ($uuid,
          |          ${user.email},
          |          ${user.phone},
          |          crypt(${user.password}, gen_salt('bf', 8)))
      """.stripMargin.update.run.map(_ => uuid)
  }


  def getUserByCredentials(email: String, password: String): doobie.ConnectionIO[Option[SavedUser]] =
    sql"""|SELECT id, email, phone FROM users
          |  WHERE email = $email AND password = crypt($password, password)
       """.stripMargin.query[SavedUser].option

}

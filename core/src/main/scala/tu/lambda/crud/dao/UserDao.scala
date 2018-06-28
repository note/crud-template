package tu.lambda.crud.dao

import tu.lambda.crud.entity.{SavedUser, User, UserId}
import doobie.ConnectionIO
import doobie.implicits._
import doobie.postgres.implicits._
import tu.lambda.crud.utils.UUIDGenerator


trait UserDao {
  def saveUser(user: User)(implicit uuidGen: UUIDGenerator): ConnectionIO[UserId]
  def getUserByCredentials(email: String, password: String): ConnectionIO[Option[SavedUser]]
}

object UserDao extends UserDao {
  def saveUser(user: User)(implicit uuidGen: UUIDGenerator): ConnectionIO[UserId] = {
    val uuid = uuidGen.generate()

    sql"""INSERT INTO users (id, email, phone, password)
          |  VALUES ($uuid,
          |          ${user.email},
          |          ${user.phone},
          |          crypt(${user.password}, gen_salt('bf', 8)))
      """.stripMargin.update.run.map(_ => UserId(uuid))
  }


  def getUserByCredentials(email: String, password: String): ConnectionIO[Option[SavedUser]] =
    sql"""SELECT id, email, phone FROM users
          |  WHERE email = $email AND password = crypt($password, password)
       """.stripMargin.query[SavedUser].option

}

package tu.lambda.crud.dao

import tu.lambda.crud.entity.{SavedUser, User, UserId}
import cats.implicits._
import doobie.ConnectionIO
import doobie.implicits._
import doobie.postgres.implicits._
import doobie.postgres._
import doobie.postgres.sqlstate
import tu.lambda.crud.utils.UUIDGenerator
import tu.lambda.crud.db.meta._


trait UserDao {
  def saveUser(user: User)(implicit uuidGen: UUIDGenerator): ConnectionIO[Option[UserId]]
  def getUserByCredentials(email: String, password: String): ConnectionIO[Option[SavedUser]]
}

object UserDao extends UserDao {
  def saveUser(user: User)(implicit uuidGen: UUIDGenerator): ConnectionIO[Option[UserId]] = {
    val uuid = uuidGen.generate()

    sql"""INSERT INTO users (id, email, phone, password)
          |  VALUES ($uuid,
          |          ${user.email},
          |          ${user.phone},
          |          crypt(${user.password}, gen_salt('bf', 8)))
      """.stripMargin.update.run
      .attemptSomeSqlState {
        case sqlstate.class23.UNIQUE_VIOLATION => ()
      }
      .map(_.fold(_ => None, _ => UserId(uuid).some))
  }


  def getUserByCredentials(email: String, password: String): ConnectionIO[Option[SavedUser]] =
    sql"""SELECT id, email, phone FROM users
          |  WHERE email = $email AND password = crypt($password, password)
       """.stripMargin.query[SavedUser].option

  // does not reflect that insert may fail but it's more convenient for calling from console
  def talkSaveUser(user: User)(implicit uuidGen: UUIDGenerator): ConnectionIO[UserId] = {
    val uuid = uuidGen.generate()

    sql"""INSERT INTO users (id, email, phone, password)
         |  VALUES ($uuid,
         |          ${user.email},
         |          ${user.phone},
         |          crypt(${user.password}, gen_salt('bf', 8)))
      """.stripMargin.update.run
      .map(_ => UserId(uuid))
  }
}

package tu.lambda.crud.dao

import tu.lambda.crud.entity.{ SavedUser, User }

import doobie.implicits._

import tu.lambda.crud.db.meta._

object UserDao {
  def saveUser(user: User) =
    sql"""|INSERT INTO users (email, phone, password)
          |  VALUES (${user.email},
          |          ${user.phone},
          |          crypt(${user.password}, gen_salt('bf', 8)))
      """.stripMargin.update

  def getUserByCredentials(email: String, password: String) =
    sql"""|SELECT id, email, phone FROM users
          |  WHERE email = $email AND password = crypt($password, password)
       """.stripMargin.query[SavedUser]

}

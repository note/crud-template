package tu.lambda.crud.dao

//import doobie._
//import doobie.implicits._
//import cats.implicits._
//import net.michalsitko.crud.entity.{ SavedUser, User }

object UserDao {
  //  def saveUser(user: User) =
  //    sql"""|INSERT INTO users (email, phone, password)
  //          |  VALUES (${user.email},
  //          |          ${user.phone},
  //          |          crypt('12345', gen_salt('bf', 8)))
  //      """.stripMargin.update
  //
  //  def getUserByCredentials(email: String, password: String) =
  //    sql"""|SELECT id, email, phone FROM users
  //          |  WHERE email = $email AND password = crypt($password, password)
  //       """.stripMargin.query[SavedUser]

}

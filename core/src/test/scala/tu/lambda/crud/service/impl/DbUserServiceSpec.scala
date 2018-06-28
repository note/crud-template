package tu.lambda.crud.service.impl

import java.util.UUID

import cats.data.NonEmptyList
import cats.free.Free
import cats.implicits._
import doobie.free.connection.ConnectionOp
import org.scalatest.WordSpec
import org.scalatest.concurrent.ScalaFutures
import tu.lambda.crud.dao.UserDao
import tu.lambda.crud.entity.{SavedUser, User, UserId}
import tu.lambda.crud.service.UserService.UserSaveFailure
import tu.lambda.crud.service.UserService.UserSaveFailure._
import tu.lambda.crud.utils.UUIDGenerator
import tu.lambda.scalatest.utils.PowerMatchers

class DbUserServiceSpec extends WordSpec with PowerMatchers with ScalaFutures {

  val newUserId   = UserId(UUID.randomUUID())
  val correctUser = User("abc@example.com", "123456123", "abcdef")

  val service = {
    val dao = new UserDao {
      override def saveUser(user: User)(implicit uuidGen: UUIDGenerator): doobie.ConnectionIO[UserId] =
        Free.pure[ConnectionOp, UserId](newUserId)

      override def getUserByCredentials(email: String, password: String): doobie.ConnectionIO[Option[SavedUser]] = ???
    }

    new DbUserService(dao, null)(UUIDGenerator.default)
  }

  "DbUserService.save" should {
    val save = (service.save _).andThen(_.apply(null).unsafeToFuture().futureValue)

    "return SavedUser for correct input" in {
      val res = save(correctUser)

      assert(res == SavedUser.fromUser(newUserId, correctUser).asRight[NonEmptyList[UserSaveFailure]])
    }

    "validate email" in {
      val res = save(correctUser.copy(email = "aa"))

      assert(res == NonEmptyList.of(IncorrectEmail).asLeft[SavedUser])
    }

    "validate password length" in {
      val res = save(correctUser.copy(password = ""))

      assert(res == NonEmptyList.of(PasswordTooShort).asLeft[SavedUser])
    }

    "validate password format" in {
      val res = save(correctUser.copy(password = "abcdef a"))

      assert(res == NonEmptyList.of(PasswordContainsWhiteSpace).asLeft[SavedUser])
    }

    "return combined errors" in {
      val res = save(correctUser.copy(email = "a", password = "a a"))

      assert(res == NonEmptyList.of(IncorrectEmail, PasswordContainsWhiteSpace, PasswordTooShort).asLeft[SavedUser])
    }
  }
}

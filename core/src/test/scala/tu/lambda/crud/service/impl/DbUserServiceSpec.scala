package tu.lambda.crud.service.impl

import java.util.UUID

import cats.data.NonEmptyList
import cats.free.Free
import cats.implicits._
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

  def service(createdUserId: Option[UserId]) = {
    val dao = new UserDao {
      override def saveUser(user: User)(implicit uuidGen: UUIDGenerator) =
        Free.pure(createdUserId)

      override def getUserByCredentials(email: String, password: String) = ???
    }

    new DbUserService(dao, null)(UUIDGenerator.default)
  }

  "DbUserService.save" should {
    def saveReturning(createdUserId: Option[UserId]) =
      (service(createdUserId).save _).andThen(_.apply(null).unsafeToFuture().futureValue)

    val save = saveReturning(newUserId.some)

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

    "validate if email already exists in database" in {
      val res = saveReturning(None)(correctUser)

      assert(res == NonEmptyList.of(EmailAlreadyExists).asLeft[SavedUser])
    }

    "return combined errors" in {
      val res = save(correctUser.copy(email = "a", password = "a a"))

      assert(res == NonEmptyList.of(IncorrectEmail, PasswordContainsWhiteSpace, PasswordTooShort).asLeft[SavedUser])
    }
  }
}

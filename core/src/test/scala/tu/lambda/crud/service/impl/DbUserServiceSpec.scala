package tu.lambda.crud.service.impl

import java.util.UUID

import cats.data.NonEmptyList
import cats.free.Free
import cats.implicits._
import doobie.free.connection.ConnectionOp
import org.scalactic.{Explicitly, Tolerance}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{DiagrammedAssertions, WordSpec}
import tu.lambda.crud.dao.{UUIDGenerator, UserDao}
import tu.lambda.crud.entity.{SavedUser, User, UserId}
import tu.lambda.crud.service.UserService.UserSaveFailure
import tu.lambda.crud.service.UserService.UserSaveFailure._

import scala.concurrent.duration._

trait PowerMatchers extends DiagrammedAssertions with Tolerance with Explicitly

class DbUserServiceSpec extends WordSpec with PowerMatchers with ScalaFutures {

  val timeout = 200.millis
  val service = DbUserService

  val newUserId = UUID.randomUUID()

  val dao = new UserDao {
    override def saveUser(user: User)(implicit uuidGen: UUIDGenerator): doobie.ConnectionIO[UUID] =
      Free.pure[ConnectionOp, UUID](newUserId)

    override def getUserByCredentials(email: String, password: String): doobie.ConnectionIO[Option[SavedUser]] = ???
  }

  val correctUser = User("abc@example.com", "123456123", "abcdef")

  "DbUserService.save" should {
    val save = (service.save(dao, UUIDGenerator.default)_)
      .andThen(_.apply(null).unsafeToFuture().futureValue)

    "return SavedUser for correct input" in {
      val res = save(correctUser)

      assert(res == SavedUser.fromUser(UserId(newUserId), correctUser).asRight[NonEmptyList[UserSaveFailure]])
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

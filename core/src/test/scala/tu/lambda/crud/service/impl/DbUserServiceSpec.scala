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
import tu.lambda.crud.service.UserService.UserSaveFailure.IncorrectEmail

import scala.concurrent.Await
import scala.concurrent.duration._

trait PowerMatchers extends DiagrammedAssertions with Tolerance with Explicitly

class DbUserServiceSpec extends WordSpec with PowerMatchers with ScalaFutures {
//  override implicit def patienceConfig: PatienceConfig =
//    super.patienceConfig
//      .copy(timeout = scaled(Span(500, Millis)), interval = scaled(Span(50, Millis)))

  val timeout = 200.millis
  val service = DbUserService

  val newUserId = UUID.randomUUID()

  val dao = new UserDao {
    override def saveUser(user: User)(implicit uuidGen: UUIDGenerator): doobie.ConnectionIO[UUID] =
      Free.pure[ConnectionOp, UUID](newUserId)

    override def getUserByCredentials(email: String, password: String): doobie.ConnectionIO[Option[SavedUser]] = ???
  }

  val correctUser = User("abc@example.com", "123456123", "abcdef")

  val save = service.save(dao, UUIDGenerator.default)_

  "DbUserService" should {

    "return SavedUser for correct input" in {
      val res = Await.result(save(correctUser).apply(null).unsafeToFuture(), timeout)

      // TODO: futureValue fails to compile...
      assert(res == SavedUser.fromUser(UserId(newUserId), correctUser).asRight[NonEmptyList[UserSaveFailure]])
    }

    "validate email" in {
      val user = correctUser.copy(email = "aa")

      val res = Await.result(save(user).apply(null).unsafeToFuture(), timeout)

      assert(res == NonEmptyList.of(IncorrectEmail).asLeft[SavedUser])
    }
  }
}

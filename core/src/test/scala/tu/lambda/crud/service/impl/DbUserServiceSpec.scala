package tu.lambda.crud.service.impl

import java.sql.Connection
import java.util.UUID

import cats.data.{Kleisli, NonEmptyList}
import cats.effect.IO
import cats.free.Free
import cats.implicits._
import doobie.free.connection.ConnectionOp
import org.scalactic.{Explicitly, Tolerance}
import org.scalatest.concurrent.ScalaFutures
import org.scalatest.{DiagrammedAssertions, WordSpec}
import tu.lambda.crud.dao.{UUIDGenerator, UserDao}
import tu.lambda.crud.entity.{SavedUser, User, UserId}
import tu.lambda.crud.service.UserService
import tu.lambda.crud.service.UserService.UserSaveFailure

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

  "DbUserService" should {

    "return SavedUser for correct input" in {
      val op: Kleisli[IO, Connection, Either[NonEmptyList[UserService.UserSaveFailure], SavedUser]] = service.save(dao, UUIDGenerator.default)(correctUser)

      // TODO: futureValue fails to compile...
      assert(Await.result(op.apply(null).unsafeToFuture(), timeout) == SavedUser.fromUser(UserId(newUserId), correctUser).asRight[NonEmptyList[UserSaveFailure]])

    }

//    "validate email" in {
//      val user = correctUser.copy(email = "aa")
//
//      val res = Await.result(service.save(user), timeout)
//
//      res should equal(Validated.Invalid(NonEmptyList.of(IncorrectEmail)))
//    }
  }
}

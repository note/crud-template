package tu.lambda.routes

import java.sql.Connection
import java.util.UUID

import akka.http.scaladsl.model.{StatusCodes, _}
import akka.http.scaladsl.server.MalformedRequestContentRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.data.{Kleisli, NonEmptyList}
import cats.effect.IO
import cats.implicits._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import doobie.util.transactor.{Strategy, Transactor}
import io.circe.Json
import io.circe.parser._
import monix.execution.Scheduler
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import tu.lambda.crud.aerospike._
import tu.lambda.crud.entity.{SavedUser, User, UserId}
import tu.lambda.crud.service.UserService
import tu.lambda.crud.service.UserService.UserSaveFailure.IncorrectEmail
import tu.lambda.crud.service.UserService._
import tu.lambda.crud.service.impl.AppContext
import tu.lambda.entity.Credentials

class UserRouteSpec extends WordSpec with Matchers with BeforeAndAfterAll with FailFastCirceSupport with ScalatestRouteTest {
  implicit val scheduler: Scheduler = monix.execution.Scheduler.Implicits.global

  "UserRoute POST /users" should {
    "return saved user if input is correct" in new Context {
      val json =
        """
          |{
          |	"email": "aa@example.com",
          |	"phone": "111222345",
          |	"password": "MyPassword"
          |}
        """.stripMargin

      val postRequest = Post("/users", entity = HttpEntity(ContentTypes.`application/json`, json))

      postRequest ~> userRoute.route ~> check {
        status should equal(StatusCodes.Created)

        // this way formatting and ordering of fields does not matter and we do not rely on formatters
        // UUIDGeneratorin test assertions
        val expectedJson =
          parse(s"""
          |{
          | "id": "${userId.id.toString}",
          |	"email": "aa@example.com",
          |	"phone": "111222345"
          |}
        """.stripMargin).right.get
        entityAs[Json] should equal(expectedJson)
      }
    }

    "return error messages for incorrect JSON" in new Context {
      override def saveResult(user: User) =
        IO.pure(NonEmptyList.of(IncorrectEmail).asLeft[SavedUser])

      val json =
        """
          |{F
          |	"email": "aa",
        """.stripMargin

      val postRequest = Post("/users", entity = HttpEntity(ContentTypes.`application/json`, json))

      postRequest ~> userRoute.route ~> check {
        rejections.size should equal(1)
        rejections.head should matchPattern { case _: MalformedRequestContentRejection => }
      }
    }

    "return error messages for inccorect input" in new Context {
      override def saveResult(user: User) =
        IO.pure(NonEmptyList.of(IncorrectEmail).asLeft[SavedUser])

      val json =
        """
          |{
          |	"email": "aa",
          |	"phone": "111222345",
          |	"password": "asfplrk"
          |}
        """.stripMargin

      val postRequest = Post("/users", entity = HttpEntity(ContentTypes.`application/json`, json))

      postRequest ~> userRoute.route ~> check {
        status should equal(StatusCodes.BadRequest)

        val expectedJson =
          parse(s"""
                   |{
                   |	"messages": ["Incorrect email"]
                   |}
        """.stripMargin).right.get
        entityAs[Json] should equal(expectedJson)
      }
    }

  }

  "UserRoute POST /users/login" should {
    def postRequest(creds: Credentials) = {
      val json =
        s"""
           |{
           |	"email": "${creds.email}",
           |	"password": "${creds.password}"
           |}
        """.stripMargin

      Post("/users/login", entity = HttpEntity(ContentTypes.`application/json`, json))
    }

    "return User if credentials are correct" in new Context {
      postRequest(correctCreds) ~> userRoute.route ~> check {
        status should equal(StatusCodes.OK)

        val expectedJson =
          parse(s"""
                   |{
                   |  "userId": "${userId.id.toString}",
                   |	"token": "${token.toString}"
                   |}
        """.stripMargin).right.get
        entityAs[Json] should equal(expectedJson)
      }
    }

    "return NOT FOUND if credentials are not correct" in new Context {
      postRequest(incorrectCreds) ~> userRoute.route ~> check {
        status should equal(StatusCodes.NotFound)
      }
    }

    "return InternalServerError if getting user failed" in new Context {
      val failingCreds = correctCreds.copy(password = "somethingIncorrect")

      postRequest(failingCreds) ~> userRoute.route ~> check {
        status should equal(StatusCodes.InternalServerError)
      }
    }
  }

  trait Context {
    val userId          = UserId(UUID.randomUUID)
    val correctCreds    = Credentials("correct", "correct")
    val incorrectCreds  = Credentials("incorrect", "incorrect")
    val token           = UUID.randomUUID()

    // TODO: it's super ugly, https://github.com/tpolecat/doobie/issues/460
    val s = Strategy.void
    implicit val transactor: Transactor[IO] = Transactor.fromConnection[IO](null).copy(strategy0 = s)
    val aerospikeClient = new AerospikeClientBase {
      override def insert(key: Key, bin: Bin)(implicit policy: WritePolicy): IO[Unit] = ???
      override def read(key: Key, binName: String): IO[Option[String]] = ???
    }
    implicit val appCtx: AppContext = AppContext(transactor, aerospikeClient)

    def saveResult(user: User) =
      IO.pure(SavedUser.fromUser(userId, user).asRight[NonEmptyList[UserSaveFailure]])

    val userService = new UserService {
      override def save(user: User): Kleisli[IO, Connection, Either[NonEmptyList[UserSaveFailure], SavedUser]] =
        Kleisli.liftF {
          saveResult(user)
        }

      override def login(email: String, password: String) =
        Kleisli.liftF {
          Credentials(email, password) match {
            case `correctCreds` =>
              IO.pure(UserSession(userId, token).some)
            case `incorrectCreds` =>
              IO.pure(Option.empty[UserSession])
            case _ =>
              IO.raiseError(new RuntimeException("some exception"))
          }
        }
    }

    val userRoute = new UserRoute(userService)
  }
}

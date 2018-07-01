package tu.lambda.routes

import java.util.UUID

import akka.http.scaladsl.model.{StatusCodes, _}
import akka.http.scaladsl.server.MalformedRequestContentRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.data.Kleisli
import cats.effect.IO
import cats.implicits._
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import doobie.util.transactor.{Strategy, Transactor}
import io.circe.Json
import io.circe.parser._
import org.scalatest.{BeforeAndAfterAll, Matchers, WordSpec}
import tu.lambda.crud.AppContext
import tu.lambda.crud.aerospike._
import tu.lambda.crud.entity.{SavedUser, Token, User, UserId}
import tu.lambda.crud.service.UserService
import tu.lambda.crud.service.UserService.UserSaveFailure.IncorrectEmail
import tu.lambda.crud.service.UserService._
import tu.lambda.entity.Credentials
import cats.data.Validated.invalidNel

import scala.concurrent.duration._

class UserRouteSpec extends WordSpec with Matchers with BeforeAndAfterAll with FailFastCirceSupport with ScalatestRouteTest {

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

      val postRequest = Post("/users").withEntity(HttpEntity(ContentTypes.`application/json`, json))

      postRequest ~> userRoute.route ~> check {
        status should equal(StatusCodes.Created)

        val expectedJson =
          parse(s"""
          |{
          | "id": "${userId.toString}",
          |	"email": "aa@example.com",
          |	"phone": "111222345"
          |}
        """.stripMargin).right.get
        entityAs[Json] should equal(expectedJson)
      }
    }

    "return error messages for incorrect JSON" in new Context {
      override def saveResult(user: User) =
        IO.pure(invalidNel[UserSaveFailure, SavedUser](IncorrectEmail))

      val json =
        """
          |{F
          |	"email": "aa",
        """.stripMargin

      val postRequest = Post("/users").withEntity(HttpEntity(ContentTypes.`application/json`, json))

      postRequest ~> userRoute.route ~> check {
        rejections.size should equal(1)
        rejections.head should matchPattern { case _: MalformedRequestContentRejection => }
      }
    }

    "return error messages for inccorect input" in new Context {
      override def saveResult(user: User) =
        IO.pure(invalidNel[UserSaveFailure, SavedUser](IncorrectEmail))

      val json =
        """
          |{
          |	"email": "aa",
          |	"phone": "111222345",
          |	"password": "asfplrk"
          |}
        """.stripMargin

      val postRequest = Post("/users").withEntity(HttpEntity(ContentTypes.`application/json`, json))

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

      Post("/users/login").withEntity(HttpEntity(ContentTypes.`application/json`, json))
    }

    "return User if credentials are correct" in new Context {
      postRequest(correctCreds) ~> userRoute.route ~> check {
        status should equal(StatusCodes.OK)

        val expectedJson =
          parse(s"""
                   |{
                   |  "userId": "${userId.toString}",
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
    val token           = Token(UUID.randomUUID())

    implicit val transactor: Transactor[IO] =
      Transactor.fromConnection[IO](null).copy(strategy0 = Strategy.void)

    val aerospikeClient = new AerospikeClientBase {
      override def insert(key: Key, bin: Bin)(implicit policy: WritePolicy): IO[Unit] = ???
      override def read(key: Key, binName: String): IO[Option[String]] = ???
    }
    implicit val appCtx: AppContext = AppContext(transactor, aerospikeClient)

    def saveResult(user: User) =
      IO.pure(SavedUser.fromUser(userId, user).validNel[UserSaveFailure])

    val userService = new UserService {
      override def save(user: User) =
        Kleisli.liftF {
          saveResult(user)
        }

      override def login(expiration: Duration)(email: String, password: String) =
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

    val userRoute = new UserRoute(userService, 2.hours)
  }
}

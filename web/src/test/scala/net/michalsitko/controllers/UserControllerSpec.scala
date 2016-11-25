package net.michalsitko.controllers

import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.data.ValidatedNel
import cats.data.Validated._
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.Json
import io.circe.parser._
import net.michalsitko.crud.entity.{ User, SavedUser, UserId }
import net.michalsitko.crud.service.{ IncorrectEmail, UserSaveError, UserService }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

import scala.concurrent.Future

class UserControllerSpec extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll with CirceSupport {

  "UserController POST" should {
    "return saved user if input is correct" in new Context {
      val json =
        """
          |{
          |	"email": "aa@example.com",
          |	"phone": "111222345",
          |	"password": "asfplrk"
          |}
        """.stripMargin

      val postRequest = Post("/user", entity = HttpEntity(ContentTypes.`application/json`, json))

      postRequest ~> userController.route ~> check {
        status should equal(StatusCodes.OK)

        // this way formatting and ordering of fields does not matter and we do not rely on formatters
        // in test assertions
        val expectedJson =
          parse(s"""
          |{
          |	"password": "asfplrk",
          | "id": "${userId.id.toString}",
          |	"email": "aa@example.com",
          |	"phone": "111222345"
          |}
        """.stripMargin).right.get
        entityAs[Json] should equal(expectedJson)
      }
    }

    "return error messages for inccorect input" in new Context {
      override def saveResult(user: User): Future[ValidatedNel[UserSaveError, SavedUser]] =
        Future.successful(invalidNel(IncorrectEmail))

      val json =
        """
          |{
          |	"email": "aa",
          |	"phone": "111222345",
          |	"password": "asfplrk"
          |}
        """.stripMargin

      val postRequest = Post("/user", entity = HttpEntity(ContentTypes.`application/json`, json))

      postRequest ~> userController.route ~> check {
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

  trait Context {
    val userId = UserId(UUID.randomUUID)
    def saveResult(user: User): Future[ValidatedNel[UserSaveError, SavedUser]] =
      Future.successful(valid(SavedUser.fromUser(userId, user)))

    val userService = new UserService {
      override def save(user: User): Future[ValidatedNel[UserSaveError, SavedUser]] =
        saveResult(user)

      override def get(userId: UserId): Future[Option[SavedUser]] = ???
    }

    val userController = new UserController(userService)
  }
}

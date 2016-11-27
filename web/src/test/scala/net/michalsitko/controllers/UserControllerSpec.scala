package net.michalsitko.controllers

import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.server.MalformedRequestContentRejection
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.data.Validated._
import cats.data.ValidatedNel
import de.heikoseeberger.akkahttpcirce.CirceSupport
import io.circe.Json
import io.circe.parser._
import net.michalsitko.crud.entity.{ SavedUser, User, UserId }
import net.michalsitko.crud.service.UserService
import net.michalsitko.crud.service.UserService._
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

    "return error messages for incorrect JSON" in new Context {
      override def saveResult(user: User): Future[ValidatedNel[UserSaveError, SavedUser]] =
        Future.successful(invalidNel(IncorrectEmail))

      val json =
        """
          |{
          |	"email": "aa",
        """.stripMargin

      val postRequest = Post("/user", entity = HttpEntity(ContentTypes.`application/json`, json))

      postRequest ~> userController.route ~> check {
        rejections.size should equal(1)
        rejections.head should matchPattern { case _: MalformedRequestContentRejection => }
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

  "UserController GET" should {
    "return user if exists" in new Context {
      Get(s"/user/${userId.id.toString}") ~> userController.route ~> check {
        status should equal(StatusCodes.OK)

        val expectedJson =
          parse(s"""
                   |{
                   |	"password": "asfplrk",
                   |  "id": "${userId.id.toString}",
                   |	"email": "aa@example.com",
                   |	"phone": "111222333"
                   |}
        """.stripMargin).right.get
        entityAs[Json] should equal(expectedJson)
      }
    }

    "return NOT FOUND if user with given id not exists" in new Context {
      Get(s"/user/d32335b1-d67e-46a2-840d-346b802c1ba5") ~> userController.route ~> check {
        status should equal(StatusCodes.NotFound)
      }
    }

    "return if getting user failed" in new Context {
      Get(s"/user/$failingUserId") ~> userController.route ~> check {
        status should equal(StatusCodes.InternalServerError)
      }
    }
  }

  trait Context {
    val userId = UserId(UUID.randomUUID)
    val failingUserId = UserId(UUID.randomUUID)

    def saveResult(user: User): Future[ValidatedNel[UserSaveError, SavedUser]] =
      Future.successful(valid(SavedUser.fromUser(userId, user)))

    val userService = new UserService {
      override def save(user: User): Future[ValidatedNel[UserSaveError, SavedUser]] =
        saveResult(user)

      override def get(id: UserId): Future[Option[SavedUser]] = id match {
        case `userId` =>
          val user = User("aa@example.com", "111222333", "asfplrk")
          Future.successful(Some(SavedUser.fromUser(userId, user)))
        case `failingUserId` =>
          Future.failed(new NoSuchElementException)
        case _ =>
          Future.successful(None)
      }
    }

    val userController = new UserController(userService)
  }
}

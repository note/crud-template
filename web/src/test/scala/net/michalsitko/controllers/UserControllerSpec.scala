package net.michalsitko.controllers

import java.util.UUID

import akka.http.scaladsl.model._
import akka.http.scaladsl.testkit.ScalatestRouteTest
import cats.data.ValidatedNel
import cats.data.Validated._
import net.michalsitko.crud.entity.{ User, SavedUser, UserId }
import net.michalsitko.crud.service.{ UserSaveError, UserService }
import org.scalatest.{ BeforeAndAfterAll, Matchers, WordSpec }

import scala.concurrent.Future

class UserControllerSpec extends WordSpec with Matchers with ScalatestRouteTest with BeforeAndAfterAll {

  "UserController POST" should {
    "works" in new Context {
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
      }
    }
  }

  trait Context {
    val userService = new UserService {
      override def save(user: User): Future[ValidatedNel[UserSaveError, SavedUser]] =
        Future.successful(valid(SavedUser.fromUser(UserId(UUID.randomUUID), user)))

      override def get(userId: UserId): Future[Option[SavedUser]] = ???
    }

    val userController = new UserController(userService)
  }
}

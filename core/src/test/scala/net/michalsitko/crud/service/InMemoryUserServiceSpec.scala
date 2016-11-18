package net.michalsitko.crud.service

import cats.data.{ NonEmptyList, Validated }
import net.michalsitko.crud.entity.User
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.Await
import scala.concurrent.duration._

class InMemoryUserServiceSpec extends WordSpec with Matchers {
  val service = new InMemoryUserService

  val timeout = 200.millis

  "InMemoryUserService" should {
    "validate" in {
      val user = User("abc", "123", "a a")

      val res = Await.result(service.save(user), timeout)

      res should equal(Validated.Invalid(NonEmptyList.of(IncorrectEmail, PasswordTooShort)))
    }
  }
}

package net.michalsitko.crud.service

import cats.data.{ NonEmptyList, Validated }
import net.michalsitko.crud.entity.User
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.Await
import scala.concurrent.duration._

class InMemoryUserServiceSpec extends WordSpec with Matchers {
  val service = new InMemoryUserService

  val timeout = 100.millis

  "InMemoryUserService" should {
    "validate email" in new Context {
      val user = correctUser.copy(email = "aa")

      val res = Await.result(service.save(user), timeout)

      res should equal(Validated.Invalid(NonEmptyList.of(IncorrectEmail)))
    }

    "validate password length" in new Context {
      val user = correctUser.copy(password = "")

      val res = Await.result(service.save(user), timeout)

      res should equal(Validated.Invalid(NonEmptyList.of(PasswordTooShort)))
    }

    "validate password format" in new Context {
      val user = correctUser.copy(password = "abcdef a")

      val res = Await.result(service.save(user), timeout)

      res should equal(Validated.Invalid(NonEmptyList.of(PasswordContainsWhiteSpace)))
    }

    "return combined errors" in new Context {
      val user = correctUser.copy(email = "a", password = "a a")

      val res = Await.result(service.save(user), timeout)

      res should equal(Validated.Invalid(NonEmptyList.of(IncorrectEmail, PasswordContainsWhiteSpace, PasswordTooShort)))
    }
  }

  trait Context {
    val correctUser = User("abc@example.com", "123456123", "abcdef")
  }
}

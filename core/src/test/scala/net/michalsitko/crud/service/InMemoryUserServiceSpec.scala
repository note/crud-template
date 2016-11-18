package net.michalsitko.crud.service

import java.util.UUID

import cats.data.Validated.Valid
import cats.data.{ NonEmptyList, Validated }
import net.michalsitko.crud.entity.{ UserId, SavedUser, User }
import org.scalatest.{ Matchers, WordSpec }

import scala.concurrent.Await
import scala.concurrent.duration._

class InMemoryUserServiceSpec extends WordSpec with Matchers {
  val timeout = 100.millis

  "InMemoryUserService.save" should {
    "return SavedUser for correct input" in new Context {
      val Valid(res) = Await.result(service.save(correctUser), timeout)

      res should equal(SavedUser.fromUser(res.id, correctUser))
    }

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

  "InMemoryUserService.get" should {
    "return user if exists" in new Context {
      val Valid(savedUser) = Await.result(service.save(correctUser), timeout)

      val res = Await.result(service.get(savedUser.id), timeout)

      res should equal(Some(savedUser))
    }

    "return None if not exists" in new Context {
      val res = Await.result(service.get(UserId(UUID.randomUUID())), timeout)

      res should equal(None)
    }
  }

  trait Context {
    val service = new InMemoryUserService

    val correctUser = User("abc@example.com", "123456123", "abcdef")
  }
}

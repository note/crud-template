package net.michalsitko.crud.service

import net.michalsitko.crud.entity.User
import org.scalatest.{ Matchers, FlatSpec }

import scala.concurrent.Await
import scala.concurrent.duration._

class DefaultUserServiceSpec extends FlatSpec with Matchers {
  val service = new DefaultUserService

  val timeout = 200.millis

  "Hello" should "have tests" in {
    val user = User("abc", "123", "")

    val res = Await.result(service.save(user), timeout)

    println("bazinga")
    println(res)
  }
}

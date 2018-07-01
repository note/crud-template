import doobie._
import doobie.implicits._
import cats._
import cats.effect._
import cats.implicits._
import io.circe._, io.circe.generic.semiauto._, io.circe.parser._, io.circe.syntax._

import tu.lambda.crud.entity._

val user = User("email@email.com", "123000321", "pass")
val savedUser = SavedUser.fromUser(UserId(java.util.UUID.randomUUID), user)

println("Loaded custom amm_predef")

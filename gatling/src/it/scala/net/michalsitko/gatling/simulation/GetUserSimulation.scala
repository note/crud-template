package net.michalsitko.gatling.simulation

import io.gatling.core.Predef._
import io.gatling.http.Predef._
import net.michalsitko.gatling.config.GatlingHttpConfig
import pureconfig._
import scala.concurrent.duration._

class GetUserSimulation extends Simulation with RequestTemplates {
  val config = loadConfig[GatlingHttpConfig]("gatling") match {
    case Right(config) =>
      config
    case Left(failures) =>
      val errorString = failures.toList.mkString(", ")
      throw new RuntimeException(s"Cannot load gatling config. Errors: $errorString")
  }

  val httpConf = http.baseURL(s"${config.scheme}://${config.host}:${config.port}")

  val scn = scenario("Get User endpoint")
    .exec(http("Add User")
        .post("/user")
        .body(createUserBody)
        .asJSON
        .check(
          jsonPath("$.id").saveAs("userId")
        )
    )

  setUp(scn.inject(rampUsers(50) over (20.seconds)).protocols(httpConf))
}

trait RequestTemplates {
  val createUserBody =
    StringBody(
      s"""
         |{
         |  "email": "aa@example.com",
         |	"phone": "123432123",
         |	"password": "asfplrk"
         |}
       """.stripMargin)
}

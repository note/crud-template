package tu.lambda

import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import org.flywaydb.core.Flyway
import org.scalactic.{Explicitly, Tolerance}
import org.scalatest.{DiagrammedAssertions, WordSpec}
import tu.lambda.crud.aerospike.AerospikeClient
import tu.lambda.entity.Credentials
import tu.lambda.routes.{BookmarkRoute, UserRoute}

trait PowerMatchers extends DiagrammedAssertions with Tolerance with Explicitly

class IntegrationSpec extends WordSpec with PowerMatchers with FailFastCirceSupport with ScalatestRouteTest {

  "crud app" should {
    "work" in new Context {
      prepareAerospike()
      prepareDb()

      signInReq ~> routes ~> check {
        assert(status === StatusCodes.Created)

        addBookmarksReq("incorrect") ~> routes ~> check {
          assert(status === StatusCodes.Unauthorized)

          loginReq ~> routes ~> check {
            assert(status === StatusCodes.OK)

            val resp = entityAs[Json]

            val token = (for {
              respObj <- resp.asObject
              tokJson <- respObj("token")
              tok     <- tokJson.asString
            } yield tok).get

            addBookmarksReq(token) ~> routes ~> check {
              assert(status === StatusCodes.Created)

              Get("/bookmarks").withHeaders(header("Authorization", token)) ~> routes ~> check {
                assert(status === StatusCodes.OK)

                assert(entityAs[Json].asArray.get.size === 1)
              }
            }
          }
        }
      }
    }
  }

  trait Context extends Services with Payloads {
    val routes = {
      import akka.http.scaladsl.server.Directives._

      val userRoute     = new UserRoute(userService)
      val bookmarkRoute = new BookmarkRoute(bookmarkService)

      userRoute.route ~ bookmarkRoute.route
    }

    def prepareDb(): Unit = {
      val f = new Flyway()
      f.setDataSource(config.db.url, config.db.user, config.db.password)
      f.clean()

      // this is hacky but I am not aware of any documented way of figuring out when `clean` completed...
      Thread.sleep(500)
      f.migrate()
    }

    def prepareAerospike(): Unit = {
      val aerospikeClient = new AerospikeClient(config.aerospike)
      aerospikeClient.truncate(config.aerospike.namespace)
    }
  }

}

trait Payloads extends RequestBuilding {
  val creds = Credentials(email = "aa@example.com", password = "MyPassword")

  val signInReq = {
    val json =
      s"""
         |{
         |	"email": "${creds.email}",
         |	"phone": "111222345",
         |	"password": "${creds.password}"
         |}
          """.stripMargin

    Post("/users").withEntity(HttpEntity(ContentTypes.`application/json`, json))
  }

  val loginReq = {
    val json =
      s"""
         |{
         |	"email": "${creds.email}",
         |	"password": "${creds.password}"
         |}
        """.stripMargin

    Post("/users/login").withEntity(HttpEntity(ContentTypes.`application/json`, json))
  }

  def addBookmarksReq(token: String) = {
    val json =
      s"""
         |{
         |	"url": "https://tpolecat.github.io/doobie/",
         |	"description": "Doobie github microsite"
         |}
        """.stripMargin

    HttpHeader.parse("Authorization", token)
    Post("/bookmarks").withEntity(HttpEntity(ContentTypes.`application/json`, json)).withHeaders(header("Authorization", token))
  }

  def header(name: String, value: String) =
    HttpHeader.parse(name, value) match {
      case ParsingResult.Ok(h, _) => h
      case _ => throw new RuntimeException(s"cannot create header for $name and $value")
    }

}
package tu.lambda

import akka.http.scaladsl.model.HttpHeader.ParsingResult
import akka.http.scaladsl.model.{ContentTypes, HttpEntity, HttpHeader, StatusCodes}
import akka.http.scaladsl.testkit.ScalatestRouteTest
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import io.circe.Json
import org.flywaydb.core.Flyway
import org.scalactic.{Explicitly, Tolerance}
import org.scalatest.{DiagrammedAssertions, WordSpec}
import pureconfig.loadConfig
import tu.lambda.config.AppConfig
import tu.lambda.crud.aerospike.AerospikeClient
import tu.lambda.entity.Credentials
import tu.lambda.routes.{BookmarkRoute, UserRoute}

trait PowerMatchers extends DiagrammedAssertions with Tolerance with Explicitly

class IntegrationSpec extends WordSpec with PowerMatchers with FailFastCirceSupport with ScalatestRouteTest {
  val config = loadConfig[AppConfig].right.get

  val aerospikeClient = new AerospikeClient(config.aerospike)
  aerospikeClient.truncate(config.aerospike.namespace)

  val f = new Flyway()
  f.setDataSource(config.db.url, config.db.user, config.db.password)
  f.clean()
  // TODO: it's hacky
  Thread.sleep(500)
  f.migrate()

  val services = new Services {}

  "app" should {
    val creds = Credentials(email = "aa@example.com", password = "MyPassword")

    "work" in new Context {
      val signinReq = {
        val json =
          s"""
            |{
            |	"email": "${creds.email}",
            |	"phone": "111222345",
            |	"password": "${creds.password}"
            |}
          """.stripMargin

        Post("/users", entity = HttpEntity(ContentTypes.`application/json`, json))
      }

      val loginReq = {
        val json =
          s"""
             |{
             |	"email": "${creds.email}",
             |	"password": "${creds.password}"
             |}
        """.stripMargin

        Post("/users/login", entity = HttpEntity(ContentTypes.`application/json`, json))
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

      signinReq ~> routes ~> check {
        assert(status === StatusCodes.Created)

        addBookmarksReq("incorrect") ~> routes ~> check {
          assert(status === StatusCodes.Unauthorized)

          loginReq ~> routes ~> check {
            assert(status  === StatusCodes.OK)

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

  trait Context extends Services {
    import akka.http.scaladsl.server.Directives._

    val userRoute     = new UserRoute(services.userService)
    val bookmarkRoute = new BookmarkRoute(services.bookmarkService)
    val routes = userRoute.route ~ bookmarkRoute.route

    def header(name: String, value: String) =
      HttpHeader.parse(name, value) match {
        case ParsingResult.Ok(h, _) => h
        case _ => throw new RuntimeException(s"cannot create header for $name and $value")
      }
  }
}

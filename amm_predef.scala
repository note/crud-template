import doobie._
import doobie.implicits._
import cats._
import cats.effect._
import cats.implicits._
import io.circe._
import io.circe.generic.semiauto._
import io.circe.parser._
import io.circe.syntax._
import tu.lambda.crud.config.DbConfig
import tu.lambda.crud.entity._

val user      = User("email@email.com", "123000321", "pass")
val savedUser = SavedUser.fromUser(UserId(java.util.UUID.randomUUID), user)
val bookmark  = Bookmark(new java.net.URL("http://google.com"), "some desc")

def flywayMigrate(config: DbConfig) = {
  import org.flywaydb.core.Flyway

  val f = new Flyway()
  f.setDataSource(config.url, config.user, config.password)
  f.migrate()
}

println("Loaded custom amm_predef")

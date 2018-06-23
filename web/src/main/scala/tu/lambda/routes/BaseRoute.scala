package tu.lambda.routes

import java.sql.Connection

import akka.http.scaladsl.server.Directives
import cats.data.Kleisli
import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import doobie.util.transactor.Transactor.Aux
import tu.lambda.format.JsonFormats

import scala.concurrent.Future

trait BaseRoute extends Directives
  with StrictLogging
  with FailFastCirceSupport
  with JsonFormats {

  implicit class ToFuture[T](thunk: Kleisli[IO, Connection, T]) {
    def exec(implicit transactor: Aux[IO, Unit]): Future[T] = transactor.exec.apply(thunk).unsafeToFuture()
  }
}

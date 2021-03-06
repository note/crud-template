package tu.lambda.routes

import java.sql.Connection

import akka.http.scaladsl.server.Directives
import cats.data.{EitherT, Kleisli}
import cats.effect.IO
import com.typesafe.scalalogging.StrictLogging
import de.heikoseeberger.akkahttpcirce.FailFastCirceSupport
import doobie.util.transactor.Transactor
import tu.lambda.crud.AppContext
import tu.lambda.crud.service.impl.BookmarkError
import tu.lambda.format.JsonFormats

import scala.concurrent.Future

trait BaseRoute extends Directives
  with StrictLogging
  with FailFastCirceSupport
  with JsonFormats {

  implicit class ToFuture[T](thunk: Kleisli[IO, Connection, T]) {
    def exec(implicit transactor: Transactor[IO]): Future[T] = transactor.exec.apply(thunk).unsafeToFuture()
  }

  implicit class AppCtxToFuture[T](thunk: Kleisli[IO, AppContext, T]) {
    def exec(implicit ctx: AppContext): Future[T] = {
      thunk.apply(ctx).unsafeToFuture()
    }
  }

  implicit class ToFuture2[T](thunk: Kleisli[EitherT[IO, BookmarkError, ?], AppContext, T]) {
    def exec(implicit ctx: AppContext): Future[Either[BookmarkError, T]] =
      thunk.apply(ctx).value.unsafeToFuture()
  }
}

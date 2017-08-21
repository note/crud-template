package net.michalsitko.generic

import akka.NotUsed
import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{HttpRequest, HttpResponse, StatusCodes}
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream._
import akka.stream.scaladsl.{Flow, Sink, Source}
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import net.michalsitko.crud.service.impl.InMemoryUserService
import net.michalsitko.{MyRetry, State}

import scala.util.{Failure, Success, Try}

object WebServer extends AnyRef with Services with StrictLogging with RequestBuilding {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("crud-template-http-system")
    implicit val materializer = ActorMaterializer()

    // TODO: Check if services/controllers should use ActorSystem's EC
    implicit val ec = system.dispatcher

    logger.info("Initializing application ...")

    def request(code: Int) = Get(s"http://localhost:8080/version?code=$code")
    val httpPool = Http().cachedHostConnectionPool[State[(HttpRequest, Long)]]("localhost", 8080)

    val withRetry: Flow[(HttpRequest, State[(HttpRequest, Long)]), (Try[HttpResponse], Long), NotUsed] = RetryWithExponentialBackoff(2)(httpPool)

    val mainFlow =
      Source(List((request(500), 88), (request(200), 88), (request(501), 88), (request(201), 88)))
        .via(withRetry)

    val resF = mainFlow.runWith(Sink.seq)

    resF.onComplete { res =>
      println("completed")
      println(res.map(_.map(_._1)))

      materializer.shutdown()
      system.terminate()
    }
  }
}

trait Services {
  val userService = new InMemoryUserService
}

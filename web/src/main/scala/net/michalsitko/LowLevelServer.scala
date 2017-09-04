package net.michalsitko

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.model._
import akka.stream.ActorMaterializer
import akka.stream.scaladsl.Flow

import scala.concurrent.Future
import scala.concurrent.duration._

object LowLevelServer {
  implicit val system = ActorSystem()
  implicit val materializer = ActorMaterializer()
  implicit val executionContext = system.dispatcher

  val httpEcho = {
    println("creating httpEcho")
    Flow[HttpRequest]
      .map { request =>
        println("bazinga next request")
        // simple streaming (!) "echo" response:
        val timeoutMs = request.headers.find { h =>
          h.name() == "Timeout"
        }.get.value().toInt
        timeoutMs.millis
      }
      .mapAsync(8) { duration =>
        val response = HttpResponse(entity = HttpEntity(ContentTypes.`text/plain(UTF-8)`, duration.toCoarsest.toString))
        akka.pattern.after(duration, system.scheduler)(Future.successful(response))
      }
  }

  def main(args: Array[String]): Unit = {
    val (host, port) = ("localhost", 8080)
    val serverSource = Http().bind(host, port)

    serverSource
      .runForeach { con =>
        println("bazinga calling handleWith")
        con.handleWith(httpEcho)
      }
  }
}

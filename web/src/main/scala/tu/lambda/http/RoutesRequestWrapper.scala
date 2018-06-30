package tu.lambda.http

import akka.http.scaladsl.model.{HttpResponse, StatusCodes}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.ExceptionHandler
import com.typesafe.scalalogging.StrictLogging

/**
  * Directly inspired and mostly c&p of https://github.com/softwaremill/bootzooka/blob/d75c0764e832859f3d68e83255106d3be31844e6/backend/src/main/scala/com/softwaremill/bootzooka/common/api/RoutesRequestWrapper.scala
  */
trait RoutesRequestWrapper extends StrictLogging {

  private val exceptionHandler = ExceptionHandler {
    case e: Exception =>
      println("here")
      logger.error(s"Exception during client request processing: ${e.getMessage}", e)
      complete(HttpResponse(StatusCodes.InternalServerError).withEntity("Internal server error"))
  }

  val requestWrapper = handleExceptions(exceptionHandler)
}

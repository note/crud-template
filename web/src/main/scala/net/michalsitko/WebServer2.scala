//package net.michalsitko
//
//import akka.actor.ActorSystem
//import akka.http.scaladsl.Http
//import akka.http.scaladsl.client.RequestBuilding
//import akka.http.scaladsl.model.{ HttpResponse, StatusCodes }
//import akka.stream.ActorMaterializer
//import akka.stream.scaladsl.{ RestartFlow, Sink, Source }
//import com.typesafe.scalalogging.StrictLogging
//
//import scala.concurrent.duration._
//import scala.util.{ Failure, Success, Try }
//
//object WebServer2 extends AnyRef with Services with StrictLogging with RequestBuilding {
//  def main(args: Array[String]) {
//    implicit val system = ActorSystem("crud-template-http-system")
//    implicit val materializer = ActorMaterializer()
//
//    // TODO: Check if services/controllers should use ActorSystem's EC
//    implicit val ec = system.dispatcher
//
//    logger.info("Initializing application ...")
//
//    def extract(response: Try[HttpResponse], state: State): (Try[HttpResponse], Long) =
//      (response, state.screeningId)
//
//    val extractResponseAndScreeningId = (extract _).tupled
//
//    val request = Get("http://localhost:8080/version?code=500")
//
//    val httpPool = {
//      val rawPool = Http().cachedHostConnectionPool[State]("localhost", 8080)
//
//      // here we define what Failure means
//      val poolWithFailureDetection = rawPool.map {
//        case (Success(response), state) if response.status == StatusCodes.InternalServerError =>
//          (Failure(new RuntimeException("internal server error")), state)
//        case anyOther => anyOther
//      }
//
//      // this can be moved to some commons and will be applicable as long as `poolWithFailureDetection` is of proper type
//      RestartFlow.withBackoff(2.seconds, 20.seconds, 0)(() => poolWithFailureDetection)
//    }
//
//    val mainFlow = Source(List((request, 88)))
//      .map(r => (r._1, State(r._1, None, 3, r._2)))
//      .via(httpPool)
//      // state information is useless for rest of processing pipeline - get rid of it
//      .map(extractResponseAndScreeningId)
//
//    val resF = mainFlow.runWith(Sink.seq)
//
//    resF.onComplete { res =>
//      println("completed")
//      println(res.map(_.headOption.map(_._1)))
//
//      materializer.shutdown()
//      system.terminate()
//    }
//  }
//}

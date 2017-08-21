package net.michalsitko

import akka.actor.ActorSystem
import akka.http.scaladsl.Http
import akka.http.scaladsl.client.RequestBuilding
import akka.http.scaladsl.model.{ HttpRequest, HttpResponse, StatusCodes }
import akka.http.scaladsl.settings.ConnectionPoolSettings
import akka.stream._
import akka.stream.contrib.Retry
import akka.stream.scaladsl.{ Flow, Sink, Source }
import akka.stream.stage._
import com.typesafe.config.ConfigFactory
import com.typesafe.scalalogging.StrictLogging
import net.michalsitko.crud.service.impl.InMemoryUserService
import net.michalsitko.generic.Services

import scala.concurrent.duration._
import scala.util.{ Failure, Success, Try }

case class State(request: HttpRequest, attemptsLeft: Int, attemptsPerformed: Int, screeningId: Long)

class ExponentialBackoff extends GraphStage[FlowShape[(HttpRequest, State), (HttpRequest, State)]] {
  // inlets/outlets names ("MyFilter.in" and "MyFilter.out" in this case)
  // serves mostly for internal diagnostic messages in case of failures
  val input = Inlet[(HttpRequest, State)]("MyFilter.in")
  val output = Outlet[(HttpRequest, State)]("MyFilter.out")
  //  val randomGenerator = Random

  override def shape: FlowShape[(HttpRequest, State), (HttpRequest, State)] = FlowShape(input, output)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) {
    setHandler(input, new InHandler {
      override def onPush(): Unit = {
        val element = grab(input)
        if (element._2.attemptsPerformed == 0) {
          push(output, element)
        } else {
          val timeToWait = Math.pow(2.0, element._2.attemptsPerformed - 1).toInt.seconds
          // is it safe to use element as timer key? or should we enrich it wich something unique?
          scheduleOnce(element, timeToWait)
          pull(input)
        }
      }
    })

    setHandler(output, new OutHandler {
      override def onPull() = {
        if (!hasBeenPulled(input)) {
          pull(input)
        }
      }
    })

    override protected def onTimer(element: Any): Unit = {
      push(output, element.asInstanceOf[(HttpRequest, State)])
    }
  }

}

object WebServer extends AnyRef with Services with StrictLogging with RequestBuilding {
  def main(args: Array[String]) {
    implicit val system = ActorSystem("crud-template-http-system")
    implicit val materializer = ActorMaterializer()

    // TODO: Check if services/controllers should use ActorSystem's EC
    implicit val ec = system.dispatcher

    logger.info("Initializing application ...")

    def extract(response: Try[HttpResponse], state: State): (Try[HttpResponse], Long) =
      (response, state.screeningId)

    val extractResponseAndScreeningId = (extract _).tupled

    def request(code: Int) = Get(s"http://localhost:8080/version?code=$code")

    val httpPool = {
      val backoff = new ExponentialBackoff
      val backoffFlow = Flow.fromGraph(backoff)

      val poolSettings = {
        val confRoot = ConfigFactory.load
        ConnectionPoolSettings(confRoot.getConfig("connectionPool").withFallback(confRoot))
      }

      println("settings: " + poolSettings)
      val rawPool = backoffFlow.via(Http().cachedHostConnectionPool[State]("localhost", 8080, poolSettings))
      //        Http().cachedHostConnectionPool[State]("localhost", 8080, poolSettings)

      // here we define what Failure means
      val poolWithFailureDetection =
        rawPool.map {
          case (Success(response), state) if response.status == StatusCodes.InternalServerError || response.status == StatusCodes.NotImplemented =>
            (Failure(new RuntimeException("internal server error: " + response.status.intValue())), state)
          case anyOther => anyOther
        }

      //       this can be moved to some commons and will be applicable as long as `poolWithFailureDetection` is of proper type
      MyRetry(poolWithFailureDetection) {
        case state if (state.attemptsLeft < 1) =>
          None
        case state =>
          val newState = state.copy(attemptsLeft = state.attemptsLeft - 1, attemptsPerformed = state.attemptsPerformed + 1)
          Some((state.request, newState))
      }
    }

    val mainFlow = Source(List((request(500), 88), (request(200), 88), (request(501), 88), (request(201), 88)))
      .map(r => (r._1, State(r._1, 3, 0, r._2)))
      .via(httpPool)
      // state information is useless for rest of processing pipeline - get rid of it
      .map(extractResponseAndScreeningId)

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

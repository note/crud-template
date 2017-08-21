package net.michalsitko.generic

import akka.NotUsed
import akka.http.scaladsl.model.HttpRequest
import akka.stream.scaladsl.Flow
import akka.stream.stage._
import akka.stream.{Attributes, FlowShape, Inlet, Outlet}
import net.michalsitko.MyRetry

import scala.concurrent.duration._
import scala.util.Try

case class State[T](originalElement: T, attemptsLeft: Int, attemptsPerformed: Int)

class ExponentialBackoff[T] extends GraphStage[FlowShape[State[T], State[T]]] {
  val input = Inlet[State[T]]("ExponentialBackoff.in")
  val output = Outlet[State[T]]("ExponentialBackoff.out")

  override def shape: FlowShape[State[T], State[T]] = FlowShape(input, output)

  override def createLogic(inheritedAttributes: Attributes): GraphStageLogic = new TimerGraphStageLogic(shape) {
    setHandler(input, new InHandler {
      override def onPush(): Unit = {
        val element = grab(input)
        if (element.attemptsPerformed == 0) {
          push(output, element)
        } else {
          val timeToWait = Math.pow(2.0, element.attemptsPerformed - 1).toInt.seconds
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
      push(output, element.asInstanceOf[State[T]])
    }
  }

}

trait Protocol {
  type Request
  type Response
}


trait ReqWithCorrelationId[T] {
  type Req
  type CorrelationId

  def req: Req
  def correlationId: CorrelationId
}

object ReqWithCorrelationId {
  def httpReqWithScreeningId(t: (HttpRequest, Long)) = new ReqWithCorrelationId[(HttpRequest, Long)] {
    override type Req = HttpRequest
    override type CorrelationId = Long

    override def req: Req = t._1

    override def correlationId: CorrelationId = t._2
  }
}


object RetryWithExponentialBackoff {
  // I - (In, Ctx)
  def apply[I : ReqWithCorrelationId, O](retriesNumber: Int)(flow: Flow[(I, State[I]), (Try[O], State[I]), NotUsed]): Flow[(I, State[I]), (Try[O], I), NotUsed] = {
    val backoffFlow: Flow[State[I], State[I], NotUsed] = {
      val backoff = new ExponentialBackoff[I]
      Flow.fromGraph(backoff)
    }

    val withBackoff: Flow[(I, State[I]), (Try[O], State[I]), NotUsed] = Flow[(I, State[I])]
      .map { case (_, state) => state }
      .via(backoffFlow)
      .map(state => (state.originalElement, state))
      .via(flow)
//      .via(flow)

    val withRetry = MyRetry (withBackoff) {
      case state if (state.attemptsLeft < 1) =>
        None
      case state =>
        val newState = state.copy(attemptsLeft = state.attemptsLeft - 1, attemptsPerformed = state.attemptsPerformed + 1)
        Some((state.originalElement, newState))
    }

    Flow.fromGraph(withRetry)
      .map(t => (t._1, t._2.originalElement))

//    val f: Flow[I, State[I], NotUsed] = Flow[I].map(el => State(el, retriesNumber + 1, 0))
//
//    val withBackoff = f.via(backoffFlow).map(state => (state.originalElement, state))
//
//    val f2: Flow[I, O, NotUsed] = withBackoff.via(flow)
//
//    MyRetry(f2) {
//      case state if (state.attemptsLeft < 1) =>
//        None
//      case state =>
//        val newState = state.copy(attemptsLeft = state.attemptsLeft - 1, attemptsPerformed = state.attemptsPerformed + 1)
//        Some((state.request, newState))
//    }
  }
}

package net.michalsitko.generic

import akka.NotUsed
import akka.stream._
import akka.stream.scaladsl.Flow
import akka.stream.stage._
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
          // is it safe to use element as timer key? or should we enrich it with something unique?
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

object RetryWithExponentialBackoff {
  type DetectFailureFn[I, C, O] = ((Try[O], State[(I, C)])) => (Try[O], State[(I, C)])

  /**
    * It's mostly designed with akka-http's `Http().cachedHostConnectionPool` in mind
    *
    * type Correlation stands for what's described in akka-http:
    *
    * In order to allow for easy response-to-request association the flow takes in a custom, opaque context
    * object of type `T` from the application which is emitted together with the corresponding response.
    */
  def apply[I, Correlation, O, M](detectFailure: DetectFailureFn[I, Correlation, O], retriesNumber: Int)(flow: Flow[(I, State[(I, Correlation)]), (Try[O], State[(I, Correlation)]), M]) = {
    val backoffFlow = {
      val backoff = new ExponentialBackoff[(I, Correlation)]
      Flow.fromGraph(backoff)
    }

    val withBackoff = Flow[(I, State[(I, Correlation)])]
      .map { case (_, state) => state }
      .via(backoffFlow)
      .map(state => (state.originalElement._1, state))
      .via(flow)
      .map(detectFailure)

    val withRetry = MyRetry(withBackoff) {
      case state if (state.attemptsLeft < 1) =>
        None
      case state =>
        val newState = state.copy(attemptsLeft = state.attemptsLeft - 1, attemptsPerformed = state.attemptsPerformed + 1)
        Some((state.originalElement._1, newState))
    }

    val f: Flow[(I, State[(I, Correlation)]), (Try[O], Correlation), NotUsed] = Flow.fromGraph(withRetry)
      .map(t => (t._1, t._2.originalElement._2))

    Flow[(I, Correlation)]
      .map(t => (t._1, State((t._1, t._2), retriesNumber + 1, 0)))
      .via(f)
  }
}

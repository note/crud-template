package net.michalsitko

import akka.stream._
import akka.stream.scaladsl._
import akka.stream.stage._

import scala.util.{ Success, Try }

/**
 * Heavily inspired by https://github.com/akka/akka-stream-contrib/blob/master/contrib/src/main/scala/akka/stream/contrib/Retry.scala
 */
object MyRetry {

  /**
   * Retry flow factory. given a flow that produces `Try`s, this wrapping flow may be used to try
   * and pass failed elements through the flow again. More accurately, the given flow consumes a tuple
   * of `input` & `state`, and produces a tuple of `Try` of `output` and `state`.
   * If the flow emits a failed element (i.e. `Try` is a `Failure`), the `retryWith` function is fed with the
   * `state` of the failed element, and may produce a new input-state tuple to pass through the original flow.
   * The function may also yield `None` instead of `Some((input,state))`, which means not to retry a failed element.
   *
   * IMPORTANT CAVEAT:
   * The given flow must not change the number of elements passing through it (i.e. it should output
   * exactly one element for every received element). Ignoring this, will have an unpredicted result,
   * and may result in a deadlock.
   */
  def apply[I, O, S, M](flow: Graph[FlowShape[(I, S), (Try[O], S)], M])(retryWith: S => Option[(I, S)]): Graph[FlowShape[(I, S), (Try[O], S)], M] = {
    GraphDSL.create(flow) { implicit b => origFlow =>
      import GraphDSL.Implicits._

      val retry = b.add(new RetryCoordinator[I, S, O](retryWith))

      retry.out2 ~> origFlow ~> retry.in2

      FlowShape(retry.in1, retry.out1)
    }
  }

  class RetryCoordinator[I, S, O](retryWith: S => Option[(I, S)]) extends GraphStage[BidiShape[(I, S), (Try[O], S), (Try[O], S), (I, S)]] {
    val in1 = Inlet[(I, S)]("Retry.ext.in")
    val out1 = Outlet[(Try[O], S)]("Retry.ext.out")
    val in2 = Inlet[(Try[O], S)]("Retry.int.in")
    val out2 = Outlet[(I, S)]("Retry.int.out")
    override val shape = BidiShape[(I, S), (Try[O], S), (Try[O], S), (I, S)](in1, out1, in2, out2)

    override def createLogic(attributes: Attributes) = new GraphStageLogic(shape) {
      var elementsInCycle = 0
      var pending: Vector[(I, S)] = Vector.empty

      setHandler(in1, new InHandler {
        override def onPush() = {
          val is = grab(in1)
          push(out2, is)
          elementsInCycle += 1
        }

        override def onUpstreamFinish() = {
          if (elementsInCycle == 0) {
            completeStage()
          }
        }
      })

      setHandler(out1, new OutHandler {
        override def onPull() = {
          if (isAvailable(out2) && isAvailable(in1)) pull(in1)
          else pull(in2)
        }
      })

      setHandler(in2, new InHandler {
        override def onPush() = {
          grab(in2) match {
            case s @ (_: Success[O], _) => pushAndCompleteIfLast(s)
            case failure @ (_, s) => retryWith(s).fold(pushAndCompleteIfLast(failure)) { is =>
              pull(in2)
              if (isAvailable(out2)) {
                push(out2, is)
              } else pending :+= is
            }
          }
        }
      })

      def pushAndCompleteIfLast(elem: (Try[O], S)): Unit = {
        push(out1, elem)
        elementsInCycle -= 1
        if (elementsInCycle == 0) {
          completeStage()
        }
      }

      setHandler(out2, new OutHandler {
        override def onPull() = {
          if (isAvailable(out1)) {
            if (pending.nonEmpty) {
              push(out2, pending.head)
              pending = pending.tail
            } else if (!hasBeenPulled(in1) && !isClosed(in1)) {
              pull(in1)
            }
          }
        }

        override def onDownstreamFinish() = {
          //Do Nothing, intercept completion as downstream
        }
      })
    }
  }

}

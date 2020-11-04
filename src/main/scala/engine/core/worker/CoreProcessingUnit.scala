package engine.core.worker

import java.util.concurrent.Executors

import engine.breakpoint.BreakpointException
import engine.common.ITuple
import engine.common.identifier.Identifier
import engine.core.InternalActor
import engine.core.control.ControlOutputChannel
import engine.message.handlers.BreakpointHandler.BreakpointTriggered
import engine.message.handlers.InternalExceptionHandler.InternalException
import engine.core.control.promise.{PromiseBody, PromiseContext, PromiseInvocation, PromiseManager}
import engine.core.data.{DataOutputChannel, DataTransferPolicy}
import engine.core.worker.utils.{PauseSupport, RecoverySupport}
import engine.event.{DataEvent, InternalPayload}
import engine.operator.{IOperatorExecutor, InputExhausted}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.control.Breaks

trait CoreProcessingUnit {
  this: PauseSupport
    with RecoverySupport
    with DataOutputChannel
    with ControlOutputChannel
    with InternalActor
    with PromiseManager =>

  val coreLogic: IOperatorExecutor

  // DP Thread
  val dataProcessExecutor: ExecutionContextExecutor =
    ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)
  // Event Queue
  val processingQueue = new mutable.ArrayDeque[Iterator[ITuple]]
  var currentConsumingTuple: ITuple = _
  var processedCount: Long = 0L
  var generatedCount: Long = 0L
  val outputPolicies = new Array[DataTransferPolicy](0)
  var outputIterator: Iterator[ITuple] = _
  val outputtedTuples: mutable.ArrayBuffer[ITuple] = mutable.ArrayBuffer[ITuple]()

  def consume(evt: DataEvent): Unit = {
    evt match {
      case InternalPayload(tuples) =>
        synchronized {
          processingQueue.append(tuples.iterator)
          tryActivate()
        }
      case others =>
      //skip
    }
  }

  def tryActivate(): Unit = {
    if (
      isDpThreadReadyToStart &&
      processingQueue.nonEmpty
    ) {
      Future {
        //If activated, reset variables
        setDpThreadStarted()
        //continue processing
        processBatchFromInternalQueue()
      }(dataProcessExecutor)

      //wait for dp thread
      waitDpThreadToStart()
    }
  }

  def processBatch(tuples: Iterator[ITuple]): Unit = {
    //If the coreLogic is still outputting data
    outputTuples()
    while (tuples.hasNext) {
      //get the current tuple
      val i = tuples.next()
      //save the current Tuple
      currentConsumingTuple = i
      //let the coreLogic consume one tuple
      consumeOneTuple(i)
      //tuple consumed
      processedCount += 1
      //check if interrupted
      exitIfInterrupted()
      //check if the coreLogic has output tuples
      outputTuples()
    }
  }

  def exitIfInterrupted(): Unit = {
    recoverControlMessage()
    interruptOnOuterPaused()
  }

  private def consumeOneTuple(tuple: ITuple): Unit = {
    try {
      outputIterator = if (tuple != null) {
        coreLogic.processTuple(Left(tuple), 0)
      } else {
        coreLogic.processTuple(Right(InputExhausted()), 0)
      }
    } catch {
      case other: Throwable =>
        println(s"exception thrown during processing $tuple :\n $other")
        reportToControllerAndBreak(InternalException(other), PauseLevel.CoreException)
    }
  }

  private def outputTuples(): Unit = {
    if (outputIterator == null) return
    var canContinue = true
    while (canContinue) {
      try {
        canContinue = outputIterator.hasNext
        if (canContinue) {
          val nextTuple = outputIterator.next()
          //TODO: add breakpoint check here
          if (nextTuple != null) {
            outputtedTuples.addOne(nextTuple)
            generatedCount += 1
          }
        }
      } catch {
        case bp: BreakpointException =>
          println(s"breakpoint triggered: $bp")
          reportToControllerAndBreak(BreakpointTriggered(), PauseLevel.Breakpoint)
        case other: Throwable =>
          canContinue = false
          println(s"exception thrown during outputting tuples :\n $other")
          reportToControllerAndBreak(InternalException(other), PauseLevel.CoreException)
      }
      exitIfInterrupted()
    }
    // add outputtedTuples to downstream
    outputPolicies.foreach { policy =>
      policy.consumeTuples(outputtedTuples).foreach { case (to, data) =>
        sendTo(to, data)
      }
    }
    outputtedTuples.clear()
  }

  private def processBatchFromInternalQueue(): Unit = {
    Breaks.breakable {
      val event = synchronized {
        processingQueue.head
      }
      processBatch(event)
      synchronized {
        processingQueue.removeHead()
        if (processingQueue.nonEmpty) {
          Future {
            processBatchFromInternalQueue()
          }(dataProcessExecutor)
        } else {
          //nothing to process
          setDpThreadExited()
        }
      }
    }
  }

  private def reportToControllerAndBreak(message: PromiseBody[_], pauseLevel: Int): Unit = {
    schedule(message, Identifier.Controller)
    setPauseLevelAndBreak(pauseLevel)
  }

}

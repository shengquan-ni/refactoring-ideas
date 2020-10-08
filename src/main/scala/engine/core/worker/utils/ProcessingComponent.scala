//package engine.core.worker.utils
//
//import java.util.concurrent.Executors
//
//
//import engine.common.{AmberIdentifier, AmberRemoteIdentifier, LinkIdentifier, OperatorIdentifier, Tuple, WorkerIdentifier}
//import engine.core.worker.ControlPriority._
//
//import scala.collection.mutable
//import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
//import scala.util.control.Breaks
//
//
//class ProcessSupport(var coreLogic: PhysicalOperatorLogic) {
//  // DP Thread
//  val dataProcessExecutor: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)
//  // Event Queue
//  val processingQueue = new mutable.ArrayDeque[DataEvent]
//  var currentConsumingTuple: Tuple = _
//  var processedCount: Long = 0L
//  var generatedCount: Long = 0L
//  var generatedAfterAccept: Long = 0L
//
//
//  def onDataProcessingEvent(event: DataEvent): Unit = {
//    event match {
//      case InternalPayload(tuples) =>
//        onProcessBatch(tuples)
//      case others =>
//      //skip
//    }
//  }
//
//
//  def onCoreError(failedTuple: Tuple): PartialFunction[Any, Unit] = {
//    case e: Exception =>
//      generatedAfterAccept = -generatedAfterAccept
//      try {
//        coreLogic.discard(failedTuple)
//      } catch {
//        case e: Exception =>
//          // fatal error
//          // sendLayer.sendTo(controller, CorePanic(e))
//          pauseSupport.setPauseLevelAndBreak(Forced)
//      }
//      // sendLayer.sendTo(controller, BreakpointTriggered(new ExceptionBreakpoint(failedTuple,e)))
//      pauseSupport.setPauseLevelAndBreak(CoreException)
//  }
//
//
//  def exitIfInterrupted(): Unit = {
//    // recoverySupport.interruptOnRecoveryEventHit(generatedCount,processedCount)
//    pauseSupport.interruptOnOuterPaused()
//  }
//
//  //Normal data processing
//  def append(event: DataEvent): Unit = {
//    synchronized {
//      processingQueue.append(event)
//      tryActivate()
//    }
//  }
//
//  //Used by breakpoint fixing tuples
//  def prepend(event: DataEvent): Unit = {
//    synchronized {
//      processingQueue.prepend(event)
//    }
//  }
//
//  def tryActivate(): Unit = {
//    if (pauseSupport.dpThreadOuterControlLevel == No &&
//      !pauseSupport.isDpThreadRunning &&
//      processingQueue.nonEmpty) {
//      Future {
//        //If activated, reset variables
//        pauseSupport.dPThreadInnerControlLevel = No
//        pauseSupport.isDpThreadRunning = true
//        //continue processing
//        processData()
//      }(dataProcessExecutor)
//
//      //wait for dp thread
//      while (!pauseSupport.isDpThreadRunning) {}
//    }
//  }
//
//  def onProcessBatch(tuples: Iterator[Tuple]): Unit = {
//    //If the coreLogic is still outputting data
//    outputTuples()
//    while (tuples.hasNext) {
//      //get the current tuple
//      val i = tuples.next()
//      //save the current Tuple
//      currentConsumingTuple = i
//      //reset per tuple counter
//      generatedAfterAccept = 0L
//      //let the coreLogic consume one tuple
//      consumeOneTuple(i)
//      //tuple consumed
//      processedCount += 1
//      //check if interrupted
//      exitIfInterrupted()
//      //check if the coreLogic has output tuples
//      outputTuples()
//    }
//  }
//
//  private def consumeOneTuple(tuple: Tuple): Unit = {
//    try {
//      coreLogic.accept(tuple)
//    } catch {
//      onCoreError(currentConsumingTuple)
//    }
//  }
//
//  private def generateOneTuple(): Tuple = {
//    var nextTuple: Tuple = null
//    try {
//      nextTuple = coreLogic.next()
//    } catch {
//      onCoreError(currentConsumingTuple)
//    }
//    nextTuple
//  }
//
//  private def outputTuples(): Unit = {
//    exitIfInterrupted()
//    while (coreLogic.hasNext) {
//      val tuple = generateOneTuple()
//      if (generatedAfterAccept >= 0L) {
//        sendLayer.addToDownstream(tuple)
//      }
//      generatedCount += 1
//      generatedAfterAccept += 1
//      exitIfInterrupted()
//    }
//  }
//
//  private def processData(): Unit = {
//    Breaks.breakable {
//      val event = synchronized {
//        processingQueue.head
//      }
//      onDataProcessingEvent(event)
//      synchronized {
//        processingQueue.removeHead()
//        if (processingQueue.nonEmpty) {
//          Future {
//            processData()
//          }(dataProcessExecutor)
//        } else {
//          //nothing to process
//          pauseSupport.isDpThreadRunning = false
//        }
//      }
//    }
//  }
//}

package engine.core.worker.utils

import java.util.concurrent.Executors

import engine.breakpoints.LocalBreakpoint.ExceptionBreakpoint
import engine.common.{LayerIdentifier, Tuple, WorkerIdentifier}
import engine.core.messages.data.{EndFlag, PayloadBlock}
import engine.core.worker.DataProcessingEvent
import engine.operators.PhysicalOperator
import engine.core.worker.ControlPriority._
import engine.core.worker.components.WorkerSendLayerComponent

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor, Future}
import scala.util.control.Breaks

trait ProcessingComponent {
  this: WorkerMetadata with PauseComponent with WorkerSendLayerComponent with RecoveryComponent with BreakpointComponent =>

  val processSupport: ProcessSupport

  class ProcessSupport(val coreLogic:PhysicalOperator){
    // DP Thread
    val dataProcessExecutor: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)
    // Event Queue
    val processingQueue = new mutable.ArrayDeque[DataProcessingEvent]
    var currentConsumingTuple: Tuple = _
    var processedCount:Long = 0L
    var generatedCount:Long = 0L
    var generatedAfterAccept:Long = 0L
    var expectedEndFlags: Map[String,Set[WorkerIdentifier]] = _
    val receivedEndFlags: mutable.HashMap[String, mutable.HashSet[WorkerIdentifier]] = mutable.HashMap[String,mutable.HashSet[WorkerIdentifier]]()


    def onDataProcessingEvent(event:DataProcessingEvent): Unit = {
      event match {
        case PayloadBlock(senderIdentifier,tuples) =>
          onProcessBatch(senderIdentifier,tuples)
        case EndFlag(senderIdentifier) =>
          onProcessEnd(senderIdentifier)
        case others =>
          //skip
      }
    }


    def onCoreError(failedTuple:Tuple): PartialFunction[Any, Unit] ={
      case e:Exception =>
        generatedAfterAccept = -generatedAfterAccept
        try {
          coreLogic.discard(failedTuple)
        }catch{
          case e:Exception =>
            //fatal error
            // sendLayer.sendTo(controller, CorePanic(e))
            pauseSupport.setPauseLevelAndBreak(Forced)
        }
        // sendLayer.sendTo(controller, BreakpointTriggered(new ExceptionBreakpoint(failedTuple,e)))
        pauseSupport.setPauseLevelAndBreak(CoreException)
    }



    def exitIfInterrupted(): Unit ={
      // recoverySupport.interruptOnRecoveryEventHit(generatedCount,processedCount)
      pauseSupport.interruptOnOuterPaused()
    }

    //Normal data processing
    def append(event:DataProcessingEvent): Unit ={
      synchronized{
        processingQueue.append(event)
        tryActivate()
      }
      if(pauseSupport.isDpThreadRunning){
        //???
      }
    }

    //Used by breakpoint fixing tuples
    def prepend(event:DataProcessingEvent): Unit ={
      synchronized{
        processingQueue.prepend(event)
      }
    }

    def tryActivate(): Unit ={
      if(pauseSupport.dpThreadOuterControlLevel == No &&
        !pauseSupport.isDpThreadRunning &&
        processingQueue.nonEmpty){
        Future {
          //If activated, reset variables
          pauseSupport.dPThreadInnerControlLevel = No
          pauseSupport.isDpThreadRunning = true
          //continue processing
          processData()
        }(dataProcessExecutor)

        //wait for dp thread
        while(!pauseSupport.isDpThreadRunning){}
      }
    }

    def onProcessBatch(layerIdentifier: LayerIdentifier, tuples:Iterator[Tuple]): Unit ={
      //notify the core that sender changed
      coreLogic.onUpstreamChanged(layerIdentifier)
      //If the coreLogic is still outputting data
      outputTuples()
      while(tuples.hasNext){
        //get the current tuple
        val i = tuples.next()
        //save the current Tuple
        currentConsumingTuple = i
        //reset per tuple counter
        generatedAfterAccept = 0L
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

    private def consumeOneTuple(tuple:Tuple): Unit ={
      try{
        coreLogic.accept(tuple)
      }catch{
        onCoreError(currentConsumingTuple)
      }
    }

    private def generateOneTuple(): Tuple ={
      var nextTuple:Tuple = null
      try{
        nextTuple = coreLogic.next()
      }catch{
        onCoreError(currentConsumingTuple)
      }
      nextTuple
    }

    private def outputTuples(): Unit ={
      exitIfInterrupted()
      while(coreLogic.hasNext){
        val tuple = generateOneTuple()
        if(generatedAfterAccept >= 0L){
          breakpointSupport.validateOneTuple(tuple)
          sendLayer.addToDownstream(tuple)
        }
        generatedCount += 1
        generatedAfterAccept += 1
        exitIfInterrupted()
      }
    }

    private def processData(): Unit ={
      Breaks.breakable {
        val event = synchronized {
          processingQueue.head
        }
        onDataProcessingEvent(event)
        synchronized {
          processingQueue.removeHead()
          if (processingQueue.nonEmpty) {
            Future {
              processData()
            }(dataProcessExecutor)
          }else{
            //nothing to process
            pauseSupport.isDpThreadRunning = false
          }
        }
      }
    }

    def onProcessEnd(id: WorkerIdentifier): Unit ={
      val layerId = id.layerIDString
      if(receivedEndFlags.contains(layerId)){
        receivedEndFlags(layerId).add(id)
      }else{
        receivedEndFlags(layerId) = mutable.HashSet[WorkerIdentifier](id)
      }
      if(expectedEndFlags != null &&
        expectedEndFlags.contains(layerId) &&
        receivedEndFlags(layerId).toSet == expectedEndFlags(id.layerIDString)){
        // onUpstreamExhausted
        // sendLayer.sendTo(controller, ReportReceivedAll(id.toLayerIdentifier))
        coreLogic.onUpstreamExhausted(id)
        if(receivedEndFlags == expectedEndFlags){
          //end
          sendLayer.deactivate()
        }
      }
    }
  }






}

package engine.core.worker.utils

import engine.core.worker.ControlPriority._
import engine.core.worker.components.WorkerSendLayerComponent

import scala.collection.mutable

trait RecoveryComponent{
  this: WorkerSendLayerComponent with WorkerMetadata with PauseComponent =>

  val recoverySupport:RecoverySupport = new RecoverySupport

  class RecoverySupport{
//    var recoveryEvents: mutable.Queue[AmberRecoveryEvent] = new mutable.Queue[AmberRecoveryEvent]()
//
//    def interruptOnRecoveryEventHit(generatedCount:Long, processedCount:Long): Unit ={
//      if(recoveryEvents.nonEmpty){
//        var currentEvent = recoveryEvents.head
//        var recoveryHit = false
//        while(currentEvent.generatedCount == generatedCount && currentEvent.processedCount == processedCount){
//          //send this event to myself and pause myself
//          sendLayer.sendTo(self, currentEvent.event)
//          recoveryEvents.removeHead()
//          currentEvent = recoveryEvents.head
//          recoveryHit = true
//        }
//        if(recoveryHit)
//          pauseSupport.setPauseLevelAndBreak(Recovery)
//      }
//    }
//
//    def addRecoveryEvent(event: AmberRecoveryEvent): Unit ={
//      recoveryEvents.enqueue(event)
//    }
  }


}

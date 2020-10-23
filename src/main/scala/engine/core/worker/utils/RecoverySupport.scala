package engine.core.worker.utils

import engine.core.InternalActor
import engine.core.control.ControlInputChannel
import engine.core.worker.{CoreProcessingUnit, WorkerRecovery}
import engine.message.ControlRecovery.RecoveryCompleted

trait RecoverySupport {
  this: CoreProcessingUnit with WorkerRecovery with ControlInputChannel with PauseSupport with InternalActor =>

  def recoverControlMessage(): Unit ={
    val cursor = processedCount+generatedCount
    if(messageToBeRecovered.contains(cursor)){
      messageToBeRecovered(cursor).foreach{
        msg =>
          //println(s"Recovered $msg when cursor = ${cursor}")
          processControlMessageForRecovery(msg)
      }
      messageToBeRecovered.remove(cursor)
      if(messageToBeRecovered.isEmpty){
        self ! RecoveryCompleted()
      }
    }
  }
}

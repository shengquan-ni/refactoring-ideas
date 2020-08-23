package engine.core.messages.data

import engine.common.WorkerIdentifier
import engine.core.worker.{DataEvent, DataProcessingEvent, Worker}

final case class EndFlag(senderIdentifier: WorkerIdentifier) extends DataProcessingEvent

final case class EndSending() extends DataEvent {
  override def onArrive(receiver: Worker): Unit = {
    receiver.processSupport.append(EndFlag(receiver.receiveLayer.currentDataMessageSender))
  }
}





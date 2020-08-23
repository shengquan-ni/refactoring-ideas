package engine.core.messages.data

import engine.common.WorkerIdentifier
import engine.core.worker.{DataEvent, Worker}

final case class StartSending(senderIdentifier:WorkerIdentifier) extends DataEvent {
  override def onArrive(receiver: Worker): Unit = {
    //do nothing
  }
}





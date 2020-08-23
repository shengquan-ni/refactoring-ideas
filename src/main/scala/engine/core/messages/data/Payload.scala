package engine.core.messages.data

import engine.common.{Tuple, WorkerIdentifier}
import engine.core.worker.{DataEvent, DataProcessingEvent, Worker}



final case class PayloadBlock(senderIdentifier: WorkerIdentifier, tuples:Iterator[Tuple]) extends DataProcessingEvent{
  override def toString: String = {
    s"PayloadBlock(${tuples.toArray.mkString("\n")})"
  }
}



final case class Payload(tuples:Array[Tuple]) extends DataEvent {
  override def onArrive(receiver: Worker): Unit = {
    receiver.processSupport.append(PayloadBlock(receiver.receiveLayer.currentDataMessageSender,tuples.iterator))
  }
}

package engine.core.data

import engine.common.OrderingEnforcer
import engine.common.identifier.Identifier
import engine.core.data.DataInputChannel.InternalDataMessage
import engine.core.data.DataOutputChannel.DataMessageAck
import engine.core.InternalActor
import engine.core.worker.CoreProcessingUnit
import engine.event.DataEvent
import engine.message.InternalFIFOMessage

import scala.collection.mutable

object DataInputChannel{
  final case class InternalDataMessage(from:Identifier, sequenceNumber: Long, messageIdentifier: Long, command: DataEvent) extends InternalFIFOMessage
}


trait DataInputChannel {
  this: InternalActor with CoreProcessingUnit =>

  private val dataOrderingEnforcer = new mutable.AnyRefMap[Identifier,OrderingEnforcer[DataEvent]]()

  def receiveDataMessage:Receive = {
    case InternalDataMessage(from,seq,messageID,cmd) =>
      sender ! DataMessageAck(messageID)
      OrderingEnforcer.reorderMessage(dataOrderingEnforcer,from,seq,cmd) match{
        case Some(iterable) =>
          iterable.foreach(consume)
        case None =>
          // discard duplicate
      }
  }
}

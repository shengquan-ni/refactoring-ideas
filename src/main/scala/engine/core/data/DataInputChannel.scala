package engine.core.data

import engine.common.OrderingEnforcer
import engine.common.identifier.AmberIdentifier
import engine.core.data.DataInputChannel.AmberDataMessage
import engine.core.data.DataOutputChannel.DataMessageAck
import engine.core.AmberActor
import engine.core.worker.CoreProcessingUnit
import engine.event.DataEvent
import engine.message.AmberFIFOMessage

import scala.collection.mutable

object DataInputChannel{
  final case class AmberDataMessage(from:AmberIdentifier, sequenceNumber: Long, messageIdentifier: Long, command: DataEvent) extends AmberFIFOMessage
}


trait DataInputChannel {
  this: AmberActor with CoreProcessingUnit =>

  private val dataOrderingEnforcer = new mutable.AnyRefMap[AmberIdentifier,OrderingEnforcer[DataEvent]]()

  def receiveDataMessage:Receive = {
    case AmberDataMessage(from,seq,messageID,cmd) =>
      sender ! DataMessageAck(messageID)
      OrderingEnforcer.reorderMessage(dataOrderingEnforcer,from,seq,cmd).foreach(process)
  }
}

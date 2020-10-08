package engine.core

import engine.common.{AmberIdentifier, OrderingEnforcer}
import engine.core.DataInputChannel.AmberDataMessage
import engine.core.DataOutputChannel.DataMessageAck
import engine.messages.{AmberFIFOMessage, ControlEvent, DataEvent}

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

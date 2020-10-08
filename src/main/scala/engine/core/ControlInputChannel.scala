package engine.core

import engine.common.{AmberIdentifier, OrderingEnforcer}
import engine.core.ControlInputChannel.AmberControlMessage
import engine.core.ControlOutputChannel.ControlMessageAck
import engine.messages.{AmberFIFOMessage, ControlEvent}

import scala.collection.mutable


object ControlInputChannel{
  final case class AmberControlMessage(from:AmberIdentifier, sequenceNumber: Long, messageIdentifier: Long, command: ControlEvent) extends AmberFIFOMessage
}


trait ControlInputChannel {
  this: AmberActor with ControlScheduler =>

  private val controlOrderingEnforcer = new mutable.AnyRefMap[AmberIdentifier,OrderingEnforcer[ControlEvent]]()

  def receiveControlMessage:Receive = {
    case AmberControlMessage(from,seq,messageID,cmd) =>
      sender ! ControlMessageAck(messageID)
      OrderingEnforcer.reorderMessage(controlOrderingEnforcer,from,seq,cmd)
        .foreach(evt => schedule(evt))
  }

}

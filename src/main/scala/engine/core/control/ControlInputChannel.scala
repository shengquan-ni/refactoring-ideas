package engine.core.control

import engine.common.OrderingEnforcer
import engine.common.identifier.AmberIdentifier
import engine.core.control.promise.{PromiseEvent, PromiseManager}
import engine.core.InternalActor
import engine.core.control.ControlInputChannel.AmberControlMessage
import engine.core.control.ControlOutputChannel.ControlMessageAck
import engine.event.ControlEvent
import engine.message.AmberFIFOMessage

import scala.collection.mutable


object ControlInputChannel{
  final case class AmberControlMessage(from:AmberIdentifier, sequenceNumber: Long, messageIdentifier: Long, command: ControlEvent) extends AmberFIFOMessage
}


trait ControlInputChannel {
  this: InternalActor with PromiseManager =>

  private val controlOrderingEnforcer = new mutable.AnyRefMap[AmberIdentifier,OrderingEnforcer[ControlEvent]]()

  def receiveControlMessage:Receive = {
    case AmberControlMessage(from,seq,messageID,cmd) =>
      sender ! ControlMessageAck(messageID)
      OrderingEnforcer.reorderMessage(controlOrderingEnforcer,from,seq,cmd)
        .foreach{
          case promise:PromiseEvent =>
            scheduleInternal(promise)
          case other =>
            //skip
        }
  }

}

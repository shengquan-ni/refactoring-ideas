package engine.core.control

import java.util.concurrent.atomic.AtomicLong

import engine.common.identifier.Identifier
import engine.core.control.ControlInputChannel.InternalControlMessage
import engine.core.control.ControlOutputChannel.ControlMessageAck
import engine.core.InternalActor
import engine.core.network.NetworkOutputLayer
import engine.event.ControlEvent
import engine.message.{ InternalFIFOMessage, ControlRecovery }

import scala.collection.mutable

object ControlOutputChannel {
  final case class ControlMessageAck(messageIdentifier: Long)
}

trait ControlOutputChannel {
  this: InternalActor with NetworkOutputLayer with ControlRecovery =>

  private val controlMessageSeqMap = new mutable.AnyRefMap[Identifier, AtomicLong]()
  private var controlUUID = 0L
  private val controlMessageInTransit = mutable.LongMap[InternalFIFOMessage]()

  def sendTo(to: Identifier, event: ControlEvent): Unit = {
    if (to == Identifier.None) {
      return
    }
    val seqNum = controlMessageSeqMap.getOrElseUpdate(to, new AtomicLong()).getAndIncrement()
    if (!ifMessageHasSent(controlUUID)) {
      val msg = InternalControlMessage(amberID, seqNum, controlUUID, event)
      controlMessageInTransit(controlUUID) = msg
      forwardMessage(to, msg)
    }
    controlUUID += 1
  }

  def ackControlMessages: Receive = { case ControlMessageAck(messageIdentifier) =>
    if (controlMessageInTransit.contains(messageIdentifier)) {
      controlMessageInTransit.remove(messageIdentifier)
      saveOutputControlMessageID(messageIdentifier)
    }
  }

}

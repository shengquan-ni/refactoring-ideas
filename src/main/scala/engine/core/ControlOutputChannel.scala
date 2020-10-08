package engine.core

import java.util.concurrent.atomic.AtomicLong

import engine.common.AmberIdentifier
import engine.core.ControlInputChannel.AmberControlMessage
import engine.core.ControlOutputChannel.ControlMessageAck
import engine.messages.{AmberEvent, AmberFIFOMessage, AmberMessage, ControlEvent}

import scala.collection.mutable

object ControlOutputChannel{
  final case class ControlMessageAck(messageIdentifier: Long)
}



trait ControlOutputChannel {
  this: AmberActor with AmberNetworkOutputLayer =>

  private val controlMessageSeqMap = new mutable.AnyRefMap[AmberIdentifier,AtomicLong]()
  private var controlUUID = 0L
  private val controlMessageInTransit = mutable.LongMap[AmberFIFOMessage]()

  def sendTo(to:AmberIdentifier, event:ControlEvent): Unit ={
    if(to == AmberIdentifier.None){
      return
    }
    val msg = AmberControlMessage(amberID, controlMessageSeqMap.getOrElseUpdate(to,new AtomicLong()).getAndIncrement(), controlUUID, event)
    controlUUID += 1
    controlMessageInTransit(controlUUID) = msg
    forwardMessage(to,msg)
  }

  def ackControlMessages:Receive ={
    case ControlMessageAck(messageIdentifier) =>
      controlMessageInTransit.remove(messageIdentifier)
  }

}

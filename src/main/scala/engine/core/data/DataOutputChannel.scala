package engine.core.data

import java.util.concurrent.atomic.AtomicLong

import engine.common.identifier.AmberIdentifier
import engine.core.AmberActor
import engine.core.data.DataInputChannel.AmberDataMessage
import engine.core.data.DataOutputChannel.DataMessageAck
import engine.core.network.AmberNetworkOutputLayer
import engine.event.DataEvent
import engine.message.AmberFIFOMessage

import scala.collection.mutable


object DataOutputChannel{
  final case class DataMessageAck(messageIdentifier: Long)
}

trait DataOutputChannel {
  this: AmberActor with AmberNetworkOutputLayer =>

  private val dataMessageSeqMap = new mutable.AnyRefMap[AmberIdentifier,AtomicLong]()
  private var dataUUID = 0L
  private val dataMessageInTransit = mutable.LongMap[AmberFIFOMessage]()

  def sendTo(to:AmberIdentifier, event:DataEvent): Unit ={
    if(to == AmberIdentifier.None){
      return
    }
    val msg = AmberDataMessage(amberID, dataMessageSeqMap.getOrElseUpdate(to,new AtomicLong()).getAndIncrement(), dataUUID, event)
    dataUUID += 1
    dataMessageInTransit(dataUUID) = msg
    forwardMessage(to,msg)
  }

  def ackDataMessages:Receive ={
    case DataMessageAck(messageIdentifier) =>
      dataMessageInTransit.remove(messageIdentifier)
  }
}

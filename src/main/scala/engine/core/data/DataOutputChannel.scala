package engine.core.data

import java.util.concurrent.atomic.AtomicLong

import engine.common.identifier.Identifier
import engine.core.InternalActor
import engine.core.data.DataInputChannel.InternalDataMessage
import engine.core.data.DataOutputChannel.DataMessageAck
import engine.core.network.NetworkOutputLayer
import engine.event.DataEvent
import engine.message.InternalFIFOMessage

import scala.collection.mutable

object DataOutputChannel {
  final case class DataMessageAck(messageIdentifier: Long)
}

trait DataOutputChannel {
  this: InternalActor with NetworkOutputLayer =>

  private val dataMessageSeqMap = new mutable.AnyRefMap[Identifier, AtomicLong]()
  private var dataUUID = 0L
  private val dataMessageInTransit = mutable.LongMap[InternalFIFOMessage]()

  def sendTo(to: Identifier, event: DataEvent): Unit = {
    if (to == Identifier.None) {
      return
    }
    val msg = InternalDataMessage(
      amberID,
      dataMessageSeqMap.getOrElseUpdate(to, new AtomicLong()).getAndIncrement(),
      dataUUID,
      event,
    )
    dataUUID += 1
    dataMessageInTransit(dataUUID) = msg
    forwardMessage(to, msg)
  }

  def ackDataMessages: Receive = { case DataMessageAck(messageIdentifier) =>
    dataMessageInTransit.remove(messageIdentifier)
  }
}

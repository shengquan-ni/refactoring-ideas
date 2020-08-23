package engine.core.data.send.policy

import akka.actor.typed.ActorRef
import engine.common.{LayerIdentifier, Tuple}
import engine.core.data.send.ActorSender.MessageInTransit
import engine.core.messages.AmberInputMessage
import engine.core.messages.data.Payload


class RoundRobin(receiverIdentifier:LayerIdentifier, batchSize:Int, var routees: Array[ActorRef[AmberInputMessage]]) extends DataTransferPolicy(receiverIdentifier, batchSize) {
  var roundRobinIndex = 0
  var batch:Array[Tuple] = _
  var currentSize = 0


  override def noMore(): Unit = {
    if(currentSize > 0) {
      senderActor ! MessageInTransit(routees(roundRobinIndex), Payload(batch.slice(0,currentSize)))
    }
  }

  override def accept(tuple:Tuple): Unit = {
    batch(currentSize) = tuple
    currentSize += 1
    if(currentSize == batchSize) {
      currentSize = 0
      senderActor ! MessageInTransit(routees(roundRobinIndex), Payload(batch))
      roundRobinIndex = (roundRobinIndex + 1) % routees.length
      batch = new Array[Tuple](batchSize)
    }
  }

  override def initialize(): Unit = {
    batch = new Array[Tuple](batchSize)
  }
}

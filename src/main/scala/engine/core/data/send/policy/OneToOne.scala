package engine.core.data.send.policy

import akka.actor.typed.ActorRef
import engine.common.{Tuple, WorkerIdentifier}
import engine.core.data.send.ActorSender.MessageInTransit
import engine.core.messages.AmberInputMessage
import engine.core.messages.data.Payload

class OneToOne(receiverIdentifier:WorkerIdentifier, batchSize:Int, val routee:ActorRef[AmberInputMessage]) extends DataTransferPolicy(receiverIdentifier, batchSize) {
  var batch:Array[Tuple] = _
  var currentSize = 0
  lazy val routees: Array[ActorRef[AmberInputMessage]] = Array(routee)

  override def accept(tuple:Tuple): Unit= {
    batch(currentSize) = tuple
    currentSize += 1
    if(currentSize == batchSize){
      currentSize = 0
      senderActor ! MessageInTransit(routee, Payload(batch))
      batch = new Array[Tuple](batchSize)
    }
  }

  override def noMore(): Unit = {
    if(currentSize > 0){
      senderActor ! MessageInTransit(routee,  Payload(batch.slice(0,currentSize)))
    }
  }


  override def initialize(): Unit = {
    batch = new Array[Tuple](batchSize)
  }
}

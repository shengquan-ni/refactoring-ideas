package engine.core.data.send.policy

import akka.actor.typed.ActorRef
import engine.common.{LayerIdentifier, Tuple}
import engine.core.data.send.ActorSender.MessageInTransit
import engine.core.messages.AmberInputMessage
import engine.core.messages.data.Payload

class HashBasedShuffle(receiverIdentifier:LayerIdentifier, batchSize:Int, val routees:Array[ActorRef[AmberInputMessage]], val hashFunc:Tuple => Int) extends DataTransferPolicy(receiverIdentifier, batchSize) {
  var batches:Array[Array[Tuple]] = _
  var currentSizes:Array[Int] = _

  override def noMore(): Unit = {
    routees.indices.filter(currentSizes(_) > 0).foreach{
      x =>
        senderActor ! MessageInTransit(routees(x), Payload(batches(x).slice(0,currentSizes(x))))
    }
  }

  override def accept(tuple:Tuple): Unit = {
    val numBuckets = routees.length
    val index = (hashFunc(tuple) % numBuckets + numBuckets) % numBuckets
    batches(index)(currentSizes(index)) = tuple
    currentSizes(index) += 1
    if(currentSizes(index) == batchSize) {
      currentSizes(index) = 0
      senderActor ! MessageInTransit(routees(index), Payload(batches(index)))
      batches(index) = new Array[Tuple](batchSize)
    }
  }

  override def initialize(): Unit = {
    batches = new Array[Array[Tuple]](routees.length)
    for(i <- routees.indices){
      batches(i) = new Array[Tuple](batchSize)
    }
    currentSizes = new Array[Int](routees.length)
  }
}

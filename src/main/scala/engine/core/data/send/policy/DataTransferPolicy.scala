package engine.core.data.send.policy

import akka.actor.typed.ActorRef
import engine.common.{AmberIdentifier, Tuple}
import engine.core.messages.{AmberInputMessage, AmberOutputMessage}

abstract class DataTransferPolicy(val receiverIdentifier:AmberIdentifier, var batchSize:Int) extends Serializable {

  def routees:Array[ActorRef[AmberInputMessage]]

  protected var senderActor:ActorRef[AmberOutputMessage] = _

  def accept(tuple:Tuple): Unit

  def noMore(): Unit

  def initialize(): Unit

  def attachTo(senderActor: ActorRef[AmberOutputMessage]):Unit ={
    assert(senderActor != null)
    this.senderActor = senderActor
  }

}

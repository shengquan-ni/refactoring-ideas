package engine.core.worker.components

import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import engine.core.data.send.ActorSender
import engine.core.data.send.ActorSender.MessageInTransit
import engine.core.messages.{AmberEvent, AmberInputMessage, AmberOutputMessage}
import engine.core.worker.DataEvent

import scala.collection.mutable

object WorkerSender{

  def apply(): Behavior[AmberOutputMessage] = {
    Behaviors.setup(context => new WorkerSender(context))
  }
}



class WorkerSender(context: ActorContext[AmberOutputMessage]) extends ActorSender(context){

  private val dataMessageSeqMap = mutable.AnyRefMap[ActorRef[_],AtomicLong]()

  protected def sendIncomingDataMessage:PartialFunction[AmberOutputMessage,Unit] ={
    case MessageInTransit(sendTo, data:DataEvent) =>
      sendTo ! constructFIFOMessage(sendTo,dataMessageSeqMap,data)
  }


  override protected lazy val messageHandler: PartialFunction[AmberOutputMessage,Unit] =
    sendIncomingDataMessage orElse
      sendIncomingControlMessage orElse
      receiveAckMessage orElse
      discardOthers

}

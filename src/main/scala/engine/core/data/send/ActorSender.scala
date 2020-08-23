package engine.core.data.send


import java.util.concurrent.atomic.AtomicLong

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import engine.core.data.receive.ActorReceiver.AmberFIFOMessage
import engine.core.data.send.ActorSender.{Ack, MessageInTransit}
import engine.core.messages.{AmberEvent, AmberInputMessage, AmberMessage, AmberOutputMessage, ControlEvent}

import scala.collection.mutable

object ActorSender{
  //Message received by sender
  final case class MessageInTransit[T<:AmberEvent](sendTo: ActorRef[AmberInputMessage], data:T) extends AmberOutputMessage
  final case class Ack(messageIdentifier: Long) extends AmberOutputMessage

  def apply(): Behavior[AmberOutputMessage] = {
    Behaviors.setup(context => new ActorSender(context))
  }
}


class ActorSender(context: ActorContext[AmberOutputMessage])  extends AbstractBehavior[AmberOutputMessage](context) {
  protected var messageUUIDCounter = 0L
  protected val controlMessageSeqMap = new mutable.AnyRefMap[ActorRef[_],AtomicLong]()
  protected val messageInTransit = new mutable.LongMap[AmberMessage]()

  override def onMessage(msg: AmberOutputMessage): Behavior[AmberOutputMessage] = {
    messageHandler(msg)
    Behaviors.same
  }

  protected lazy val messageHandler: PartialFunction[AmberOutputMessage,Unit] = sendIncomingControlMessage orElse receiveAckMessage orElse discardOthers

  protected def discardOthers: PartialFunction[AmberOutputMessage,Unit] ={
    case message =>
      context.log.info(s"Discarding message =  $message")
  }

  protected def receiveAckMessage:PartialFunction[AmberOutputMessage,Unit] ={
    case Ack(id) =>
      if(messageInTransit.contains(id)){
        messageInTransit.remove(id)
      }
  }

  protected def sendIncomingControlMessage: PartialFunction[AmberOutputMessage,Unit] ={
    case MessageInTransit(sendTo, command:ControlEvent) =>
      sendTo ! constructFIFOMessage(sendTo,controlMessageSeqMap,command)
  }

  @inline
  protected def constructFIFOMessage(sendTo:ActorRef[AmberInputMessage], messageSeqMap: mutable.AnyRefMap[ActorRef[_],AtomicLong], command:AmberEvent): AmberInputMessage ={
    val msg = AmberFIFOMessage(context.self, controlMessageSeqMap.getOrElseUpdate(sendTo,new AtomicLong()).getAndIncrement(),messageUUIDCounter, command)
    messageInTransit(messageUUIDCounter) = msg
    messageUUIDCounter += 1
    msg
  }

  override def onSignal: PartialFunction[Signal, Behavior[AmberOutputMessage]] = {
    case PostStop =>
      onStop()
      Behaviors.stopped
  }

  protected def onStop(): Unit ={
    //clean up
  }


}
package engine.core.data.receive

import akka.actor.typed.ActorRef
import engine.common.OrderingEnforcer
import engine.core.data.receive.ActorReceiver.AmberFIFOMessage
import engine.core.messages.{AmberEvent, AmberInputMessage, ControlEvent}

import scala.collection.mutable
import scala.reflect.ClassTag

trait ReceiveLayerComponent {
  this: ActorReceiver =>
  val receiveLayer:ReceiveLayer

  class ReceiveLayer{
    protected val controlOrderingEnforcer = new mutable.AnyRefMap[ActorRef[_],OrderingEnforcer[ControlEvent]]()

    protected def receiveControlMessage: PartialFunction[AmberInputMessage,Iterable[ControlEvent]] ={
      case AmberFIFOMessage(sender,sequenceNumber, messageIdentifier, command: ControlEvent) =>
        reorderMessage(controlOrderingEnforcer,sender,sequenceNumber,command)
    }

    protected def discardOthers:PartialFunction[AmberInputMessage,Iterable[ControlEvent]] = {
      case message =>
        context.log.info(s"Discarding message =  $message")
        Iterable.empty
    }

    protected lazy val messageHandler: PartialFunction[AmberInputMessage, Iterable[AmberEvent]] = receiveControlMessage orElse discardOthers

    def onReceive(msg: AmberInputMessage):Iterable[AmberEvent] = messageHandler(msg)

    def reorderMessage[V: ClassTag](seqMap:mutable.AnyRefMap[ActorRef[_],OrderingEnforcer[V]], sender: ActorRef[_], seq:Long, command: V): Iterable[V] ={
      val entry = seqMap.getOrElseUpdate(sender,new OrderingEnforcer[V]())
      if(entry.ifDuplicated(seq)){
        Iterable.empty
      }else{
        entry.pushAndRelease(seq,command)
      }
    }
  }
}

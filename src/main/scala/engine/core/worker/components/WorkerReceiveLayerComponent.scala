package engine.core.worker.components

import akka.actor.typed.ActorRef
import engine.common.{OrderingEnforcer, WorkerIdentifier}
import engine.core.data.receive.ActorReceiver.AmberFIFOMessage
import engine.core.data.receive.{ActorReceiver, ReceiveLayerComponent}
import engine.core.messages.data.StartSending
import engine.core.messages.{AmberEvent, AmberInputMessage}
import engine.core.worker.{DataEvent, Worker}

import scala.collection.mutable


trait WorkerReceiveLayerComponent extends ReceiveLayerComponent {
  this: ActorReceiver =>

  override val receiveLayer: WorkerReceiveLayer

  class WorkerReceiveLayer extends ReceiveLayer{

    private val dataOrderingEnforcer = new mutable.AnyRefMap[ActorRef[_],OrderingEnforcer[DataEvent]]()
    private val workerLayerMapping = new mutable.AnyRefMap[ActorRef[_],WorkerIdentifier]()

    var currentDataMessageSender:WorkerIdentifier = _

    protected def receiveDataMessage: PartialFunction[AmberInputMessage, Iterable[AmberEvent]] ={
      case AmberFIFOMessage(sender,seq,id,data:DataEvent)=>
        val res = reorderMessage(dataOrderingEnforcer, sender,seq,data).map{
          evt =>
            evt match {
              case StartSending(senderIdentifier) =>
                workerLayerMapping(sender) = senderIdentifier
              case others =>
                //skip
            }
            evt
        }
        if(res.nonEmpty){
          currentDataMessageSender = workerLayerMapping(sender)
        }
        res
    }

    override lazy val messageHandler: PartialFunction[AmberInputMessage, Iterable[AmberEvent]] = receiveControlMessage orElse receiveDataMessage orElse discardOthers

  }

}

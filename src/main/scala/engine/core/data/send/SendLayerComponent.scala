package engine.core.data.send

import akka.actor.typed.ActorRef
import engine.common.WorkerIdentifier
import engine.core.messages.{AmberEvent, AmberInputMessage, AmberOutputMessage}

trait SendLayerComponent {

  val sendLayer: SendLayer

  trait SendLayer{
    val myIdentifier: WorkerIdentifier
    val senderActor: ActorRef[AmberOutputMessage]
    def sendTo(receiver:ActorRef[AmberInputMessage], event: AmberEvent): Unit
  }

}

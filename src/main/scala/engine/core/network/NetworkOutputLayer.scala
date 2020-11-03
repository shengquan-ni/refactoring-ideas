package engine.core.network

import akka.actor.{ Actor, ActorRef }
import engine.common.identifier.Identifier
import engine.core.network.NetworkOutputLayer.{ QueryActorRef, ReplyActorRef }
import engine.message.InternalFIFOMessage

import scala.collection.mutable

object NetworkOutputLayer {
  final case class QueryActorRef(id: Identifier)

  final case class ReplyActorRef(id: Identifier, ref: ActorRef)
}

trait NetworkOutputLayer {
  this: Actor =>

  private val idMap = mutable.HashMap[Identifier, ActorRef]()
  private val messageStash = mutable.HashMap[Identifier, mutable.Queue[InternalFIFOMessage]]()

  def findActorRefAutomatically: Receive = {
    case QueryActorRef(id) =>
      if (idMap.contains(id)) {
        sender ! ReplyActorRef(id, idMap(id))
      } else {
        context.parent.tell(QueryActorRef(id), sender)
      }
    case ReplyActorRef(id, ref) =>
      registerActorRef(id, ref)
  }

  def forwardMessage(to: Identifier, message: InternalFIFOMessage): Unit = {
    if (idMap.contains(to)) {
      forwardInternal(idMap(to), message)
    } else {
      messageStash.getOrElseUpdate(to, new mutable.Queue[InternalFIFOMessage]()).addOne(message)
      context.parent ! QueryActorRef(to)
    }
  }

  private def forwardInternal(to: ActorRef, message: InternalFIFOMessage): Unit = {
    to ! message
  }

  private def registerActorRef(id: Identifier, ref: ActorRef): Unit = {
    idMap(id) = ref
    if (messageStash.contains(id)) {
      val stash = messageStash(id)
      while (stash.nonEmpty) {
        forwardInternal(ref, stash.dequeue())
      }
    }
  }
}

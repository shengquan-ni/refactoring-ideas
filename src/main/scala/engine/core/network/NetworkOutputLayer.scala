package engine.core.network

import akka.actor.{Actor, ActorRef}
import engine.common.identifier.AmberIdentifier
import engine.core.network.NetworkOutputLayer.{QueryActorRef, ReplyActorRef}
import engine.message.AmberFIFOMessage

import scala.collection.mutable

object NetworkOutputLayer {
  final case class QueryActorRef(id:AmberIdentifier)

  final case class ReplyActorRef(id:AmberIdentifier, ref:ActorRef)
}

trait NetworkOutputLayer {
  this: Actor =>

  private val idMap = mutable.HashMap[AmberIdentifier,ActorRef]()
  private val messageStash = mutable.HashMap[AmberIdentifier, mutable.Queue[AmberFIFOMessage]]()

  def findActorRefAutomatically:Receive = {
    case QueryActorRef(id) =>
      if(idMap.contains(id)){
        sender ! ReplyActorRef(id,idMap(id))
      }else{
        context.parent.tell(QueryActorRef(id),sender)
      }
    case ReplyActorRef(id,ref) =>
      registerActorRef(id,ref)
  }


  def forwardMessage(to:AmberIdentifier, message: AmberFIFOMessage): Unit ={
    if(idMap.contains(to)){
      forwardInternal(idMap(to),message)
    }else{
      messageStash.getOrElseUpdate(to,new mutable.Queue[AmberFIFOMessage]()).addOne(message)
      context.parent ! QueryActorRef(to)
    }
  }


  private def forwardInternal(to:ActorRef, message:AmberFIFOMessage):Unit ={
    to ! message
  }

  private def registerActorRef(id:AmberIdentifier, ref:ActorRef): Unit ={
    idMap(id) = ref
    if(messageStash.contains(id)){
      val stash = messageStash(id)
      while(stash.nonEmpty){
        forwardInternal(ref, stash.dequeue())
      }
    }
  }
}

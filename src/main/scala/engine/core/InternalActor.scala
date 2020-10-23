package engine.core

import akka.actor.{Actor, ActorLogging, ActorRef, Stash}
import engine.common.identifier.Identifier
import engine.message.ControlRecovery


trait InternalActor extends Actor with ActorLogging with Stash with Serializable{
  val amberID:Identifier

}


package engine.core

import akka.actor.{Actor, ActorLogging, ActorRef}
import engine.common.identifier.AmberIdentifier


trait InternalActor extends Actor with ActorLogging with Serializable{
  val amberID:AmberIdentifier
}


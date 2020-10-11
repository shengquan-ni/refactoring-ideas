package engine.core

import akka.actor.{Actor, ActorLogging, ActorRef}
import engine.common.identifier.AmberIdentifier


trait AmberActor extends Actor with ActorLogging with Serializable{
  val amberID:AmberIdentifier
}


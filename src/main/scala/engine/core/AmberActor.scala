package engine.core

import akka.actor.{Actor, ActorLogging, ActorRef}
import engine.common.{AmberIdentifier, AmberRemoteIdentifier}
import engine.messages.AmberEvent

import scala.collection.mutable



abstract class AmberActor(val amberID:AmberIdentifier) extends Actor with ActorLogging

class Controller(amberID:AmberIdentifier) extends AmberActor(amberID) with ControlInputChannel with ControlOutputChannel with AmberNetworkOutputLayer with ControlScheduler {

  def ignoreOthers:Receive = {
    case msg =>
      log.info(s"Ignored message: $msg")
  }

  override def receive: Receive = findActorRefAutomatically orElse receiveControlMessage orElse ackControlMessages orElse ignoreOthers
}


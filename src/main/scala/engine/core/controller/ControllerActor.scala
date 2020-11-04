package engine.core.controller

import engine.common.identifier.Identifier
import engine.core.InternalActor
import engine.core.control.promise.PromiseManager
import engine.core.control.{ControlInputChannel, ControlOutputChannel}
import engine.core.network.NetworkOutputLayer
import engine.message.ControlRecovery

class ControllerActor(val amberID: Identifier)
  extends InternalActor
  with ControlInputChannel
  with ControlOutputChannel
  with ControllerRecovery
  with PromiseManager
  with NetworkOutputLayer {

  override def receive: Receive = {
    findActorRefAutomatically
      .orElse(receiveControlMessage)
      .orElse(ackControlMessages)
      .orElse(ignoreOthers)
  }

}

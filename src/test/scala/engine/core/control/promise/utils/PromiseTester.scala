package engine.core.control.promise.utils

import engine.common.identifier.AmberIdentifier
import engine.core.AmberActor
import engine.core.control.{ControlInputChannel, ControlOutputChannel}
import engine.core.control.promise.PromiseManager
import engine.core.network.AmberNetworkOutputLayer

class PromiseTester(val amberID:AmberIdentifier) extends AmberActor
  with ControlInputChannel with ControlOutputChannel with AmberNetworkOutputLayer with PromiseManager
  with NestedHandler with PingPongHandler with RecursionHandler with CollectHandler with ChainHandler
  with SubPromiseHandler with NoReturnHandler with DummyStateComponent {

  def ignoreOthers:Receive = {
    case msg =>
      log.info(s"Ignored message: $msg")
  }

  override def receive: Receive = findActorRefAutomatically orElse receiveControlMessage orElse ackControlMessages orElse ignoreOthers
}

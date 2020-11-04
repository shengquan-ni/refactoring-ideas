package engine.core.worker

import engine.common.identifier.Identifier
import engine.core.InternalActor
import engine.core.control.ControlInputChannel.InternalControlMessage
import engine.core.control.{ControlInputChannel, ControlOutputChannel}
import engine.core.control.ControlOutputChannel.ControlMessageAck
import engine.core.control.promise.PromiseManager
import engine.core.data.{DataInputChannel, DataOutputChannel}
import engine.core.network.NetworkOutputLayer
import engine.core.worker.utils.{PauseSupport, RecoverySupport}
import engine.event.ControlEvent
import engine.message.ControlRecovery.RecoveryCompleted
import engine.operator.IOperatorExecutor

class WorkerActor(
  val amberID: Identifier,
  val coreLogic: IOperatorExecutor,
  val withRecovery: Boolean = false,
) extends InternalActor
  with ControlInputChannel
  with ControlOutputChannel
  with NetworkOutputLayer
  with PromiseManager
  with WorkerRecovery
  with CoreProcessingUnit
  with PauseSupport
  with RecoverySupport
  with DataInputChannel
  with DataOutputChannel {

  if (withRecovery) {
    triggerRecovery()
  } else {
    resetRecovery()
  }

  coreLogic.open()

  def stashControlMessages: Receive = { case InternalControlMessage(_, _, messageID, _) =>
    sender ! ControlMessageAck(messageID)
    stash()
  }

  def returnToNormalProcessing: Receive = { case RecoveryCompleted() =>
    context.become(normalProcessing)
    unstashAll()
  }

  override def processControlEvents(iter: Iterable[ControlEvent]): Unit = {
    if (trySetOuterPauseLevel(PauseLevel.Internal)) {
      waitDpThreadToPause()
    }
    super.processControlEvents(iter)
    if (tryReleaseOuterPauseLevel(PauseLevel.Internal)) {
      tryActivate()
    }
  }

  def normalProcessing: Receive = {
    findActorRefAutomatically
      .orElse(receiveDataMessage)
      .orElse(ackDataMessages)
      .orElse(receiveControlMessage)
      .orElse(ackControlMessages)
      .orElse(ignoreOthers)
  }

  def recovering: Receive = {
    findActorRefAutomatically
      .orElse(receiveDataMessage)
      .orElse(ackDataMessages)
      .orElse(stashControlMessages)
      .orElse(ackControlMessages)
      .orElse(returnToNormalProcessing)
      .orElse(ignoreOthers)
  }

  override def receive: Receive = {
    if (withRecovery) {
      recovering
    } else {
      normalProcessing
    }
  }
}

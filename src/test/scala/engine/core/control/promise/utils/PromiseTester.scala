package engine.core.control.promise.utils

import engine.common.{ITuple, OrderingEnforcer}
import engine.common.identifier.Identifier
import engine.core.InternalActor
import engine.core.control.ControlInputChannel.InternalControlMessage
import engine.core.control.ControlOutputChannel.ControlMessageAck
import engine.core.control.{ControlInputChannel, ControlOutputChannel}
import engine.core.control.promise.{PromiseEvent, PromiseManager}
import engine.core.network.NetworkOutputLayer
import engine.core.data.{DataInputChannel, DataOutputChannel}
import engine.core.worker.utils.{PauseSupport, RecoverySupport}
import engine.core.worker.{CoreProcessingUnit, IOperatorExecutor, InputExhausted, PauseLevel, WorkerRecovery}
import engine.event.ControlEvent
import engine.message.ControlRecovery.RecoveryCompleted


class PromiseTester(val amberID:Identifier, val withRecovery:Boolean = false) extends InternalActor
  with ControlInputChannel with ControlOutputChannel with NetworkOutputLayer with PromiseManager
  with NestedHandler with PingPongHandler with RecursionHandler with CollectHandler with ChainHandler with DummyState
  with SubPromiseHandler with ExampleHandler with WorkerRecovery with CoreProcessingUnit with PauseSupport with RecoverySupport with DataInputChannel with DataOutputChannel {

  if(withRecovery){
    triggerRecovery()
  }else{
    resetRecovery()
  }

  override val coreLogic: IOperatorExecutor = new IOperatorExecutor {
    override def open(): Unit = {}

    override def close(): Unit = {}

    override def processTuple(tuple: Either[ITuple, InputExhausted], input: Int): Iterator[ITuple] = {
      tuple match{
        case Left(value) =>
          Thread.sleep(10)
          Iterator(value)
        case Right(value) =>
          Iterator.empty
      }
    }
  }

  coreLogic.open()

  def ignoreOthers:Receive = {
    case msg =>
      log.info(s"Ignored message: $msg")
  }

  def stashControlMessages:Receive = {
    case InternalControlMessage(_,_,messageID,_) =>
      sender ! ControlMessageAck(messageID)
      stash()
  }

  def returnToNormalProcessing:Receive = {
    case RecoveryCompleted() =>
      context.become(normalProcessing)
      unstashAll()
  }


  override def processControlEvents(iter: Iterable[ControlEvent]): Unit = {
    if(trySetOuterPauseLevel(PauseLevel.Internal)){
      waitDpThreadToPause()
    }
    super.processControlEvents(iter)
    if(tryReleaseOuterPauseLevel(PauseLevel.Internal)){
      tryActivate()
    }
  }

  def normalProcessing:Receive = {
    findActorRefAutomatically orElse
    receiveDataMessage orElse
    ackDataMessages orElse
    receiveControlMessage orElse
    ackControlMessages orElse
    ignoreOthers
  }

  def recovering:Receive = {
    findActorRefAutomatically orElse
    receiveDataMessage orElse
    ackDataMessages orElse
    stashControlMessages orElse
    ackControlMessages orElse
    returnToNormalProcessing orElse
    ignoreOthers
  }

  override def receive: Receive = {
    if(withRecovery){
      recovering
    }else{
      normalProcessing
    }
  }
}

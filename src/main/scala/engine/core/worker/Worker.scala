package engine.core.worker

import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import engine.common.WorkerIdentifier
import engine.core.data.receive.ActorReceiver
import engine.core.data.send.ActorSender
import engine.core.messages.{AmberEvent, AmberInputMessage, AmberOutputMessage, ControlEvent}
import engine.core.worker.Worker.WorkerInitializer
import engine.core.worker.components.{WorkerReceiveLayerComponent, WorkerSendLayerComponent, WorkerSender}
import engine.core.worker.utils.{BreakpointComponent, PauseComponent, ProcessingComponent, RecoveryComponent, WorkerMetadata}
import engine.operators.PhysicalOperator

import scala.collection.mutable



object Worker{
  case class WorkerInitializer(myIdentifier:WorkerIdentifier,
                               coreLogic:PhysicalOperator,
                               controller:ActorRef[AmberInputMessage],
                               principal:ActorRef[AmberInputMessage])

  def apply(initializer:WorkerInitializer): Behavior[AmberInputMessage] = {
    Behaviors.setup(context => new Worker(initializer,context))
  }
}



//encapsulated receiving logic and control processing logic
class Worker(initializer:WorkerInitializer, context: ActorContext[AmberInputMessage])
  extends ActorReceiver(context)
    with WorkerReceiveLayerComponent
    with WorkerMetadata
    with ProcessingComponent
    with BreakpointComponent
    with PauseComponent
    with RecoveryComponent
    with WorkerSendLayerComponent {

  private val senderActor: ActorRef[AmberOutputMessage] = context.spawnAnonymous(WorkerSender())

  override val sendLayer: WorkerSendLayer = new WorkerSendLayer(initializer.myIdentifier,senderActor)
  override val receiveLayer: WorkerReceiveLayer = new WorkerReceiveLayer()
  override val self: ActorRef[AmberInputMessage] = context.self
  override val localManager: ActorRef[AmberInputMessage] = null
  override val controller: ActorRef[AmberInputMessage] = null
  override val processSupport: ProcessSupport = new ProcessSupport(initializer.coreLogic)

  private val unResolvedControlEvents:mutable.LongMap[CollectiveControlEventForWorker] = new mutable.LongMap[CollectiveControlEventForWorker]()


  protected def processDataEvent:PartialFunction[AmberEvent,Unit] = {
    case evt: DataEvent=>
      evt.onArrive(this)
  }


  protected def beforeProcessControlEvent(controlLogic: ControlEvent): Unit = {
    controlLogic match{
      case logic:WorkerControlLogic =>
        onPause(logic.pausePriority)
    }
  }

  protected def afterProcessControlEvent(controlLogic: ControlEvent): Unit = {
    controlLogic match{
      case logic:WorkerControlLogic =>
        onResume(logic.resumePriority)
    }
  }

  private def onPause(pauseLevel:Int):Unit = {
    if (pauseSupport.trySetOuterPauseLevel(pauseLevel)) {
      while (pauseSupport.isDpThreadRunning) {
        //wait
      }
    }
  }

  private def onResume(pauseLevel: Int): Unit ={
    if(pauseSupport.tryReleaseOuterPauseLevel(pauseLevel)){
      processSupport.tryActivate()
    }
  }


  protected def processControlEvent:PartialFunction[AmberEvent,Unit] = {
    case ctrl: ControlEvent =>
      beforeProcessControlEvent(ctrl)
      ctrl match{
        case logic:CollectiveControlEventForWorker =>
          logic.onDispatch(this)
          if(!logic.isResolved) {
            unResolvedControlEvents(logic.id) = logic
          }
        case reply:BackwardControlEventForWorker =>
          //TODO: what if id is not contained
          unResolvedControlEvents(reply.id).onCollect(this,reply)
          if(unResolvedControlEvents(reply.id).isResolved) {
            unResolvedControlEvents.remove(reply.id)
          }
        case command:DirectControlEventForWorker =>
          command.onArrive(this)
        case sub:RoundTripControlEventForWorker =>
          sub.onArrive(this)
      }
      afterProcessControlEvent(ctrl)
  }


  protected override lazy val eventHandler: PartialFunction[AmberEvent,Unit] = processControlEvent orElse processDataEvent orElse discardOtherEvents


}

package engine.core.worker

import akka.actor.testkit.typed.FishingOutcome
import akka.actor.typed.ActorRef
import akka.actor.testkit.typed.scaladsl.{ScalaTestWithActorTestKit, TestProbe}
import engine.common.{LayerIdentifier, WorkerIdentifier}
import engine.core.data.receive.ActorReceiver.AmberFIFOMessage
import engine.core.messages.data.Payload
import engine.core.messages.{AmberEvent, AmberInputMessage, AmberOutputMessage, ControlEvent}
import engine.core.worker.Worker.WorkerInitializer
import engine.operators.PhysicalOperator
import org.scalatest.BeforeAndAfterEach
import org.scalatest.wordspec.AnyWordSpecLike

import scala.concurrent.duration._
import scala.collection.mutable

trait AmberWorkerTestKit extends ScalaTestWithActorTestKit with AnyWordSpecLike with BeforeAndAfterEach{

  var controlLinkMap:mutable.AnyRefMap[(ActorRef[_],ActorRef[_]),Long] = new mutable.AnyRefMap[(ActorRef[_],ActorRef[_]),Long]()
  var dataLinkMap:mutable.AnyRefMap[(ActorRef[_],ActorRef[_]),Long] = new mutable.AnyRefMap[(ActorRef[_],ActorRef[_]),Long]()
  var currentWorkflow = 0L
  var currentOperator = 0L
  var currentLayer = 0L
  var currentWorker = 0L

  def clearCounter(): Unit ={
    controlLinkMap.clear()
    dataLinkMap.clear()
  }

  override def beforeEach(): Unit = {
    clearCounter()
  }

  def dummyWorkerIdentifier: WorkerIdentifier = {
    currentWorker += 1
    new WorkerIdentifier(currentWorkflow,currentOperator,currentLayer,currentWorker)
  }

  def dummyLayerIdentifier: LayerIdentifier = {
    currentLayer += 1
    new LayerIdentifier(currentWorkflow,currentOperator,currentLayer)
  }

  def dummyController: ActorRef[AmberInputMessage] = createTestProbe[AmberInputMessage]().ref

  def dummyLocalManager: ActorRef[AmberInputMessage] = createTestProbe[AmberInputMessage]().ref

  def dummySender: ActorRef[AmberOutputMessage] = createTestProbe[AmberOutputMessage]().ref

  def mkWorker(coreLogic:PhysicalOperator, controller: ActorRef[AmberInputMessage] = dummyController, localManager: ActorRef[AmberInputMessage] = dummyLocalManager): (ActorRef[AmberInputMessage],WorkerIdentifier) ={
    val id = dummyWorkerIdentifier
    (spawn(Worker(WorkerInitializer(id,coreLogic,controller, localManager))), id)
  }

  def sendMessage(sender:ActorRef[AmberOutputMessage], receiver: ActorRef[AmberInputMessage], event:AmberEvent): Unit ={
    event match{
      case ctrl:ControlEvent =>
        receiver ! AmberFIFOMessage(sender, controlLinkMap.getOrElseUpdate((sender,receiver),0),0,event)
        controlLinkMap((sender,receiver)) += 1
      case data:DataEvent =>
        receiver ! AmberFIFOMessage(sender, dataLinkMap.getOrElseUpdate((sender,receiver),0),0,event)
        dataLinkMap((sender,receiver)) += 1
    }
  }

  def sendMessages(sender:ActorRef[AmberOutputMessage], receiver: ActorRef[AmberInputMessage], events:Seq[AmberEvent]):Unit ={
    events.foreach{
      x => sendMessage(sender,receiver,x)
    }
  }



  def expectMessages(receiver: TestProbe[AmberInputMessage], events:Seq[AmberEvent]): Unit ={
    events.foreach{
      event =>
      receiver.receiveMessage() match{
        case AmberFIFOMessage(_,_,_,cmd:ControlEvent) =>
          assert(cmd == event)
        case AmberFIFOMessage(_,_,_,data:DataEvent) =>
        data match{
          case x: Payload =>
            assert(x.tuples sameElements event.asInstanceOf[Payload].tuples)
          case other =>
            assert(data == event)
        }
      }
    }
  }

  def expectMessagesNotContain(receiver: TestProbe[AmberInputMessage], events:Set[AmberEvent], timeInterval:FiniteDuration = 30.seconds): Unit = {
    receiver.within(timeInterval){
      receiver.receiveMessage() match{
        case AmberFIFOMessage(_,_,_,cmd:ControlEvent) =>
          assert(!events.contains(cmd))
        case AmberFIFOMessage(_,_,_,data:DataEvent) =>
          assert(!events.contains(data))
      }
    }
  }

  def receiveMessagesUntil(receiver: TestProbe[AmberInputMessage], event:AmberEvent, timeInterval:FiniteDuration = 30.seconds): Unit = {
    var flag = false
    receiver.fishForMessage(timeInterval) {
      msg =>
        println(msg)
        msg match {
          case AmberFIFOMessage(_,_,_,cmd:ControlEvent) =>
            if (cmd == event) {
              flag = true
              FishingOutcome.Complete
            } else {
              FishingOutcome.Continue
            }
          case AmberFIFOMessage(_,_,_,data:DataEvent) =>
            if (data == event) {
              flag = true
              FishingOutcome.Complete
            } else {
              FishingOutcome.Continue
            }
        }
    }
    assert(flag)
  }

}

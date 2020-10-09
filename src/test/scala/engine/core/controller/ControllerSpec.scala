package engine.core.controller

import java.io.Serializable

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import engine.clustering.SingleNodeListener
import engine.common.{ActorIdentifier, AmberIdentifier, OperatorIdentifier, WorkflowIdentifier}
import engine.core.AmberNetworkOutputLayer.{QueryActorRef, ReplyActorRef}
import engine.core.ControlInputChannel.AmberControlMessage
import engine.core.{ControlScheduler, Controller}
import engine.messages.{AmberPromise, Chain, Collect, Nested, Ping, PromiseContext, PromiseEvent, Recursion, ReturnEvent}
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}
import org.scalatest.wordspec.AnyWordSpecLike

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration._

class ControllerSpec extends TestKit(ActorSystem("ControllerSpec")) with AnyWordSpecLike with BeforeAndAfterEach with BeforeAndAfterAll  {

  override def beforeAll:Unit = {
    system.actorOf(Props[SingleNodeListener],"cluster-info")
  }
  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }


  def testCallEvent[T](numActors:Int, event:AmberPromise, expectedValue:T): Unit ={
    val probe = TestProbe()
    val idMap = mutable.HashMap[AmberIdentifier,ActorRef]()
    for(i <- 0 until numActors){
      val ref = probe.childActorOf(Props(new Controller(ActorIdentifier(i))))
      idMap(ActorIdentifier(i)) = ref
    }
    idMap(AmberIdentifier.Client) = probe.ref
    probe.send(idMap(ActorIdentifier(0)), AmberControlMessage(AmberIdentifier.Client,0,0, PromiseEvent(PromiseContext(AmberIdentifier.Client,0L),event)))
    var flag = false
    probe.receiveWhile(5.minutes,2.seconds){
      case QueryActorRef(id) =>
        probe.sender() ! ReplyActorRef(id,idMap(id))
      case AmberControlMessage(_,_,_,ReturnEvent(context,value)) =>
        assert(value.asInstanceOf[T] == expectedValue)
        flag = true
      case other =>
        //skip
    }
    assert(flag)
  }



  "controller" should{

    "execute Ping Pong" in {
      testCallEvent(2, Ping(1, ActorIdentifier(1)),5)
    }

    "execute Chain" in {
      testCallEvent(10, Chain((1 to 9).map(ActorIdentifier(_))), ActorIdentifier(0))
    }

    "execute Collect" in {
      testCallEvent(4, Collect((1 to 3).map(ActorIdentifier(_))),"finished")
    }

    "execute RecursiveCall" in {
      testCallEvent(1, Recursion(0), "0")
    }

    "execute NestedCall" in {
      testCallEvent(1, Nested(5), "Hello World!")
    }

  }

}

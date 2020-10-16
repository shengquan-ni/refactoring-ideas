package engine.core.control.promise

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.testkit.{TestKit, TestProbe}
import engine.clustering.SingleNodeListener
import engine.common.identifier.{ActorIdentifier, AmberIdentifier}
import engine.core.control.ControlInputChannel.AmberControlMessage
import engine.core.control.{ControlInputChannel, ControlOutputChannel}
import engine.core.control.promise.utils.ChainHandler.Chain
import engine.core.control.promise.utils.CollectHandler.Collect
import engine.core.control.promise.utils.NestedHandler.{Nested, Pass}
import engine.core.control.promise.utils.NoReturnHandler.{NoReturnInvoker, NoReturnInvoker2}
import engine.core.control.promise.utils.PingPongHandler.Ping
import engine.core.control.promise.utils.RecursionHandler.Recursion
import engine.core.control.promise.utils.SubPromiseHandler.PromiseInvoker
import engine.core.control.promise.utils.{ChainHandler, CollectHandler, NestedHandler, NoReturnHandler, PingPongHandler, PromiseTester, RecursionHandler, SubPromiseHandler}
import engine.core.InternalActor
import engine.core.network.NetworkOutputLayer
import engine.core.network.NetworkOutputLayer.{QueryActorRef, ReplyActorRef}
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{BeforeAndAfterAll, BeforeAndAfterEach}

import scala.collection.mutable
import scala.concurrent.duration._

class PromiseSpec extends TestKit(ActorSystem("PromiseSpec")) with AnyWordSpecLike with BeforeAndAfterEach with BeforeAndAfterAll  {

  override def beforeAll:Unit = {
    system.actorOf(Props[SingleNodeListener],"cluster-info")
  }
  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  def setUp(numActors:Int, event:InternalPromise[_]*): (TestProbe, mutable.HashMap[AmberIdentifier,ActorRef]) ={
    val probe = TestProbe()
    val idMap = mutable.HashMap[AmberIdentifier,ActorRef]()
    for(i <- 0 until numActors){
      val ref = probe.childActorOf(Props(new PromiseTester(ActorIdentifier(i))))
      idMap(ActorIdentifier(i)) = ref
    }
    idMap(AmberIdentifier.Client) = probe.ref
    var seqNum = 0
    event.foreach{
      evt =>
        probe.send(idMap(ActorIdentifier(0)), AmberControlMessage(AmberIdentifier.Client,seqNum,seqNum, PromiseInvocation(mkPromiseContext(evt,seqNum),evt)))
        seqNum += 1
    }
    (probe,idMap)
  }

  def mkPromiseContext(cmd:InternalPromise[_], seqNum:Int):PromiseContext = {
     PromiseContext(AmberIdentifier.Client, seqNum)
  }

  def testPromise[T](numActors:Int, eventPairs:(InternalPromise[_],T)*): Unit ={
    val (events,expectedValues) = eventPairs.unzip
    val (probe,idMap) = setUp(numActors,events:_*)
    var flag = 0
    probe.receiveWhile(5.minutes,2.seconds){
      case QueryActorRef(id) =>
        probe.sender() ! ReplyActorRef(id,idMap(id))
      case AmberControlMessage(_,_,_,ReturnEvent(context,value)) =>
        assert(value.asInstanceOf[T] == expectedValues(context.id.toInt))
        flag += 1
      case other =>
        //skip
    }
    if(flag != expectedValues.length){
      throw new AssertionError()
    }
  }

  def testPromise(numActors:Int, event:InternalPromise[_]): Unit ={
    val (probe,idMap) = setUp(numActors,event)
    var flag = true
    probe.receiveWhile(5.minutes,2.seconds){
      case QueryActorRef(id) =>
        probe.sender() ! ReplyActorRef(id,idMap(id))
      case AmberControlMessage(_,_,_,ReturnEvent(context,value)) =>
        flag = false
      case other =>
      //skip
    }
    assert(flag)
  }



  "controller" should{

    "execute Ping Pong" in {
      testPromise(2, (Ping(1, 5, ActorIdentifier(1)),5))
    }


    "execute Ping Pong 2 times" in {
      testPromise(2, (Ping(1, 4, ActorIdentifier(1)),4), (Ping(10, 13, ActorIdentifier(1)),13))
    }

    "execute Chain" in {
      testPromise(10, (Chain((1 to 9).map(ActorIdentifier(_))), ActorIdentifier(9)))
    }

    "execute Chain 3 times" in {
      testPromise(10,
        (Chain((1 to 9).map(ActorIdentifier(_))), ActorIdentifier(9)),
        (Chain((1 to 2).map(ActorIdentifier(_))), ActorIdentifier(2)),
        (Chain((1 to 4).map(ActorIdentifier(_))), ActorIdentifier(4))
      )
    }


    "execute Collect" in {
      testPromise(4, (Collect((1 to 3).map(ActorIdentifier(_))),"finished"))
    }

    "execute RecursiveCall" in {
      testPromise(1, (Recursion(0), "0"))
    }

    "execute SubPromise" in {
      testPromise(10, (PromiseInvoker((1 to 9).map(ActorIdentifier(_))), "1"))
    }


    "execute SubPromise 3 times" in {
      testPromise(10, (PromiseInvoker((1 to 9).map(ActorIdentifier(_))), "1"),
        (PromiseInvoker((1 to 9).map(ActorIdentifier(_))), "1"),
        (PromiseInvoker((1 to 9).map(ActorIdentifier(_))), "1"))
    }


    "execute NestedCall" in {
      testPromise(1, (Nested(5), "Hello World!"))
    }

    "execute NoReturn" in {
      testPromise(2, NoReturnInvoker(ActorIdentifier(1)))
    }

    "execute NoReturn 2" in {
      val (probe,idMap) = setUp(2,NoReturnInvoker(ActorIdentifier(1)), NoReturnInvoker2(ActorIdentifier(1)), NoReturnInvoker(ActorIdentifier(1)))
      var flag = 0
      probe.receiveWhile(5.minutes,2.seconds){
        case QueryActorRef(id) =>
          probe.sender() ! ReplyActorRef(id,idMap(id))
        case AmberControlMessage(_,_,_,ReturnEvent(context,value)) =>
          assert(value == 1)
          flag += 1
        case other =>
        //skip
      }
      if(flag != 1){
        throw new AssertionError()
      }
    }

  }

}

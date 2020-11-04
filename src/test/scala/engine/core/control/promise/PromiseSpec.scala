package engine.core.control.promise

import java.io.{ FileInputStream, FileOutputStream, ObjectInputStream, ObjectOutputStream }

import akka.actor.{ ActorRef, ActorSystem, Props }
import akka.testkit.{ TestKit, TestProbe }
import com.esotericsoftware.kryo.{ Kryo, KryoException }
import com.esotericsoftware.kryo.io.Input
import com.twitter.util.FuturePool
import clustering.SingleNodeListener
import engine.common.ITuple
import engine.common.identifier.{ ActorIdentifier, Identifier }
import engine.core.control.ControlInputChannel.InternalControlMessage
import engine.core.control.{ ControlInputChannel, ControlOutputChannel }
import engine.core.control.promise.utils.ChainHandler.Chain
import engine.core.control.promise.utils.CollectHandler.Collect
import engine.core.control.promise.utils.NestedHandler.{ Nested, Pass }
import engine.core.control.promise.utils.PingPongHandler.Ping
import engine.core.control.promise.utils.RecursionHandler.Recursion
import engine.core.control.promise.utils.SubPromiseHandler.PromiseInvoker
import engine.core.control.promise.utils.{
  ChainHandler,
  CollectHandler,
  NestedHandler,
  PingPongHandler,
  PromiseTester,
  RecursionHandler,
  SubPromiseHandler,
}
import engine.core.InternalActor
import engine.core.control.promise.utils.ExampleHandler.Init
import engine.core.data.DataInputChannel.InternalDataMessage
import engine.core.network.NetworkOutputLayer
import engine.core.network.NetworkOutputLayer.{ QueryActorRef, ReplyActorRef }
import engine.event.InternalPayload
import engine.message.InternalFIFOMessage
import org.scalatest.wordspec.AnyWordSpecLike
import org.scalatest.{ BeforeAndAfterAll, BeforeAndAfterEach }

import scala.collection.mutable
import scala.concurrent.duration._

class PromiseSpec
  extends TestKit(ActorSystem("PromiseSpec"))
  with AnyWordSpecLike
  with BeforeAndAfterEach
  with BeforeAndAfterAll {

  override def beforeAll: Unit = {
    system.actorOf(Props[SingleNodeListener], "cluster-info")
  }
  override def afterAll: Unit = {
    TestKit.shutdownActorSystem(system)
  }

  def setUp(
    numActors: Int,
    event: PromiseBody[_]*
  ): (TestProbe, mutable.HashMap[Identifier, ActorRef]) = {
    val probe = TestProbe()
    val idMap = mutable.HashMap[Identifier, ActorRef]()
    for (i <- 0 until numActors) {
      val ref = probe.childActorOf(Props(new PromiseTester(ActorIdentifier(i))))
      idMap(ActorIdentifier(i)) = ref
    }
    idMap(Identifier.Client) = probe.ref
    var seqNum = 0
    event.foreach { evt =>
      probe.send(
        idMap(ActorIdentifier(0)),
        InternalControlMessage(
          Identifier.Client,
          seqNum,
          seqNum,
          PromiseInvocation(mkPromiseContext(seqNum), evt),
        ),
      )
      seqNum += 1
    }
    (probe, idMap)
  }

  def mkPromiseContext(seqNum: Int): PromiseContext = {
    PromiseContext(Identifier.Client, seqNum)
  }

  def testPromise[T](numActors: Int, eventPairs: (PromiseBody[_], T)*): Unit = {
    val (events, expectedValues) = eventPairs.unzip
    val (probe, idMap) = setUp(numActors, events: _*)
    var flag = 0
    probe.receiveWhile(5.minutes, 5.seconds) {
      case QueryActorRef(id) =>
        probe.sender() ! ReplyActorRef(id, idMap(id))
      case InternalControlMessage(_, _, _, ReturnEvent(context, value)) =>
        assert(value.asInstanceOf[T] == expectedValues(context.id.toInt))
        flag += 1
      case other =>
      //skip
    }
    if (flag != expectedValues.length) {
      throw new AssertionError()
    }
  }

  "controller" should {

    "execute Ping Pong" in {
      testPromise(2, (Ping(1, 5, ActorIdentifier(1)), 5))
    }

    "execute Ping Pong 2 times" in {
      testPromise(2, (Ping(1, 4, ActorIdentifier(1)), 4), (Ping(10, 13, ActorIdentifier(1)), 13))
    }

    "execute Chain" in {
      testPromise(10, (Chain((1 to 9).map(ActorIdentifier(_))), ActorIdentifier(9)))
    }

    "execute Chain 3 times" in {
      testPromise(
        10,
        (Chain((1 to 9).map(ActorIdentifier(_))), ActorIdentifier(9)),
        (Chain((1 to 2).map(ActorIdentifier(_))), ActorIdentifier(2)),
        (Chain((1 to 4).map(ActorIdentifier(_))), ActorIdentifier(4)),
      )
    }

    "execute Collect" in {
      testPromise(4, (Collect((1 to 3).map(ActorIdentifier(_))), "finished"))
    }

    "execute RecursiveCall" in {
      testPromise(1, (Recursion(0), "0"))
    }

    "execute SubPromise" in {
      testPromise(10, (PromiseInvoker((1 to 9).map(ActorIdentifier(_))), "1"))
    }

    "execute SubPromise 3 times" in {
      testPromise(
        10,
        (PromiseInvoker((1 to 9).map(ActorIdentifier(_))), "1"),
        (PromiseInvoker((1 to 9).map(ActorIdentifier(_))), "1"),
        (PromiseInvoker((1 to 9).map(ActorIdentifier(_))), "1"),
      )
    }

    "execute NestedCall" in {
      testPromise(1, (Nested(5), "Hello World!"))
    }

    "execute an example message" in {
      testPromise(2, (Init(), PromiseCompleted()))
    }

    "process data messages" in {
      val (probe, map) = setUp(4)
      (0 until 1000).foreach { i =>
        probe.send(
          map(ActorIdentifier(0)),
          InternalDataMessage(Identifier.Client, i, i, InternalPayload(Array(ITuple(i)))),
        )
      }

      FuturePool.unboundedPool {
        (0 until 5).foreach { i =>
          val evt = Collect((1 to 3).map(ActorIdentifier(_)))
          val ctrl = PromiseInvocation(mkPromiseContext(i), evt)
          probe.send(
            map(ActorIdentifier(0)),
            InternalControlMessage(Identifier.Client, i, 1000 + i, ctrl),
          )
          Thread.sleep(1000)
        }
      }

      probe.receiveWhile(5.minutes, 10.seconds) {
        case QueryActorRef(id) =>
          probe.sender() ! ReplyActorRef(id, map(id))
        case InternalControlMessage(_, _, _, ReturnEvent(context, value)) =>
          assert(value.asInstanceOf[String] == "finished")
        case other =>
        //skip
      }
    }

    "recover with data messages" in {
      val probe = TestProbe()
      val map = mutable.HashMap[Identifier, ActorRef]()
      // creating workers with recovery mode on
      for (i <- 0 until 4) {
        val ref = probe.childActorOf(Props(new PromiseTester(ActorIdentifier(i), true)))
        map(ActorIdentifier(i)) = ref
      }
      // replay data messages and start recovery
      map(Identifier.Client) = probe.ref
      (0 to 1000).foreach { i =>
        probe.send(
          map(ActorIdentifier(0)),
          InternalDataMessage(Identifier.Client, i, i, InternalPayload(Array(ITuple(i)))),
        )
      }
      // send more control messages after recovery
      FuturePool.unboundedPool {
        (5 until 6).foreach { i =>
          val evt = Collect((1 to 3).map(ActorIdentifier(_)))
          val ctrl = PromiseInvocation(mkPromiseContext(i), evt)
          probe.send(
            map(ActorIdentifier(0)),
            InternalControlMessage(Identifier.Client, i, 1000 + i, ctrl),
          )
          Thread.sleep(1000)
        }
      }
      probe.receiveWhile(5.minutes, 20.seconds) {
        case QueryActorRef(id) =>
          probe.sender() ! ReplyActorRef(id, map(id))
        case InternalControlMessage(_, _, _, ReturnEvent(context, value)) =>
          assert(value.asInstanceOf[String] == "finished")
        case other =>
        //skip
      }
    }

  }

}

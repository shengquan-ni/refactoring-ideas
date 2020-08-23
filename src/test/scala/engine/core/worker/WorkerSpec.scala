package engine.core.worker

import engine.common.Tuple
import engine.core.data.send.policy.OneToOne
import engine.operators.simple.{MinusOne, Source}

import scala.concurrent.duration._
import akka.util.Timeout
import engine.core.messages.data.{EndSending, Payload, StartSending}
import engine.core.messages.{AddDownStream, AmberEvent, AmberInputMessage, SetExpectedEndFlags, SourceStartToWorker}

class WorkerSpec extends AmberWorkerTestKit {

  "A worker" should {
    "generate tuples" in {
      val sender = dummySender
      val receiver = createTestProbe[AmberInputMessage]()
      val (worker,id) = mkWorker(new Source(100))
      val policy = new OneToOne(dummyWorkerIdentifier,1, receiver.ref)
      sendMessage(sender, worker, AddDownStream(policy))
      sendMessage(sender, worker, SourceStartToWorker(0,null))
      val payloads: Seq[DataEvent] = (1 until 101).map {
        x =>
          Payload(Array(Tuple(x)))
      }
      val events = StartSending(id) +: payloads :+ EndSending()
      expectMessages(receiver, events)
    }


    "process tuples" in {
      val sender = dummySender
      val receiver = createTestProbe[AmberInputMessage]()
      val (worker,id) = mkWorker(new MinusOne(0))
      val policy = new OneToOne(dummyWorkerIdentifier, 1, receiver.ref)
      sendMessage(sender, worker, SetExpectedEndFlags(Map(id.layerIDString -> Set(id))))
      sendMessage(sender, worker, AddDownStream(policy))
      val payloads: Seq[DataEvent] = (1 until 101).map {
        x =>
          Payload(Array(Tuple(x)))
      }
      val expectedPayloads: Seq[AmberEvent] = (0 until 100).map {
        x =>
          Payload(Array(Tuple(x)))
      }
      val events = StartSending(id) +: payloads :+ EndSending()
      val expectedEvents = StartSending(id) +: expectedPayloads :+ EndSending()
      sendMessages(sender, worker, events)
      expectMessages(receiver, expectedEvents)
    }
  }

}
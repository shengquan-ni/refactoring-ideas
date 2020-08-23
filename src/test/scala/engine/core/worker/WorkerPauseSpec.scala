package engine.core.worker

import engine.common.Tuple
import engine.core.data.send.policy.OneToOne
import engine.operators.simple.{MinusOne, Source}

import scala.concurrent.duration._
import akka.util.Timeout
import engine.core.messages.data.{EndSending, Payload, StartSending}
import engine.core.messages.{AddDownStream, AmberInputMessage, Pause, Resume, SetExpectedEndFlags}

class WorkerPauseSpec extends AmberWorkerTestKit {

  import ControlPriority._

  "A worker" should {
    "pause/resume once during processing" in {
      val sender = dummySender
      val receiver = createTestProbe[AmberInputMessage]()
      val (worker,id) = mkWorker(new MinusOne(0))
      val policy = new OneToOne(dummyWorkerIdentifier, 1, receiver.ref)
      sendMessage(sender, worker, SetExpectedEndFlags(Map(id.layerIDString -> Set(id))))
      sendMessage(sender, worker, AddDownStream(policy))
      expectMessages(receiver,Seq(StartSending(id)))
      sendMessage(sender, worker, Pause(User))
      val events = StartSending(id) :: Payload((1 until 20).map(Tuple(_)).toArray) :: EndSending() :: Nil
      sendMessages(sender, worker, events)
      receiver.expectNoMessage(30.seconds)
      sendMessage(sender, worker, Resume(User))
      receiveMessagesUntil(receiver, EndSending())
    }

    "pause/resume arbitrary times during processing" in {
      val sender = dummySender
      val receiver = createTestProbe[AmberInputMessage]()
      val (worker,id) = mkWorker(new MinusOne(0))
      val policy = new OneToOne(dummyWorkerIdentifier, 1, receiver.ref)
      sendMessage(sender, worker, SetExpectedEndFlags(Map(id.layerIDString -> Set(id))))
      sendMessage(sender, worker, AddDownStream(policy))
      val payloads: Seq[DataEvent] = (1 until 50000).map {
        x =>
          Payload(Array(Tuple(x)))
      }

      val events = StartSending(id) +: payloads :+ EndSending()
      sendMessages(sender, worker, events)
      for (i <- 0 until 50) {
        Thread.sleep(10)
        sendMessage(sender, worker, Pause(User))
        sendMessage(sender, worker, Resume(User))
      }
      receiveMessagesUntil(receiver, EndSending())
    }


    "be able to receive multiple pause messages" in {
      val sender = dummySender
      val receiver = createTestProbe[AmberInputMessage]()
      val (worker,id) = mkWorker(new MinusOne(0))
      val policy = new OneToOne(dummyWorkerIdentifier, 1, receiver.ref)
      sendMessage(sender, worker, SetExpectedEndFlags(Map(id.layerIDString -> Set(id))))
      sendMessage(sender, worker, AddDownStream(policy))
      val payloads: Seq[DataEvent] = (1 until 50000).map {
        x =>
          Payload(Array(Tuple(x)))
      }

      val events = StartSending(id) +: payloads :+ EndSending()
      sendMessages(sender, worker, events)
      Thread.sleep(10)
      sendMessage(sender, worker, Pause(User))
      sendMessage(sender, worker, Pause(User))
      sendMessage(sender, worker, Pause(User))
      sendMessage(sender, worker, Pause(User))
      sendMessage(sender, worker, Pause(User))
      sendMessage(sender, worker, Pause(User))
      sendMessage(sender, worker, Resume(User))
      receiveMessagesUntil(receiver, EndSending())
    }

    "be able to pause after finishing processing messages" in {
      val sender = dummySender
      val receiver = createTestProbe[AmberInputMessage]()
      val (worker,id) = mkWorker(new MinusOne(0))
      val policy = new OneToOne(dummyWorkerIdentifier, 1, receiver.ref)
      sendMessage(sender, worker, SetExpectedEndFlags(Map(id.layerIDString -> Set(id))))
      sendMessage(sender, worker, AddDownStream(policy))
      val payloads: Seq[DataEvent] = (1 until 5).map {
        x =>
          Payload(Array(Tuple(x)))
      }

      val events = StartSending(id) +: payloads :+ EndSending()
      sendMessages(sender, worker, events)
      receiveMessagesUntil(receiver, EndSending())
      sendMessage(sender, worker, Pause(User))
      sendMessage(sender, worker, Resume(User))
      sendMessage(sender, worker, Pause(User))
      sendMessage(sender, worker, Resume(User))
    }

    "be able to pause before processing messages" in {
      val sender = dummySender
      val receiver = createTestProbe[AmberInputMessage]()
      val (worker,id) = mkWorker(new MinusOne(0))
      val policy = new OneToOne(dummyWorkerIdentifier, 1, receiver.ref)
      sendMessage(sender, worker, SetExpectedEndFlags(Map(id.layerIDString -> Set(id))))
      sendMessage(sender, worker, AddDownStream(policy))
      sendMessage(sender, worker, Pause(User))
      sendMessage(sender, worker, Resume(User))
      sendMessage(sender, worker, Pause(User))
      sendMessage(sender, worker, Resume(User))
      val payloads: Seq[DataEvent] = (1 until 5).map {
        x =>
          Payload(Array(Tuple(x)))
      }

      val events = StartSending(id) +: payloads :+ EndSending()
      sendMessages(sender, worker, events)
      receiveMessagesUntil(receiver, EndSending())
    }
  }
}

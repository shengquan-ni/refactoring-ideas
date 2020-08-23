package engine.core.worker

import engine.common.Tuple
import engine.core.data.send.policy.OneToOne
import engine.core.messages.data.{EndSending, Payload, StartSending}
import engine.core.messages.{AddDownStream, AmberEvent, AmberInputMessage, ControlEvent, RemoveDownStream, SetExpectedEndFlags}
import engine.operators.simple.{MinusOne, Source}

class WorkerDataTransferSpec extends AmberWorkerTestKit {

  "A worker" should {
    "add a link during processing" in {
      val sender = dummySender
      val receiver = createTestProbe[AmberInputMessage]()
      val (worker, id) = mkWorker(new MinusOne(0))
      val policy = new OneToOne(dummyWorkerIdentifier, 1, receiver.ref)
      val events: List[AmberEvent] =
        SetExpectedEndFlags(Map(id.layerIDString -> Set(id)))::
          StartSending(id) ::
          Payload((1 until 2000).map(Tuple(_)).toArray) ::
          EndSending() ::
          Nil
      sendMessages(sender, worker, events)
      Thread.sleep(20)
      sendMessage(sender, worker, AddDownStream(policy))
      receiveMessagesUntil(receiver, EndSending())
    }

    "add a link after processing" in {
      val sender = dummySender
      val receiver = createTestProbe[AmberInputMessage]()
      val (worker, id) = mkWorker(new MinusOne(0))
      val policy = new OneToOne(dummyWorkerIdentifier, 1, receiver.ref)
      val events: List[AmberEvent] =
        SetExpectedEndFlags(Map(id.layerIDString -> Set(id)))::
          StartSending(id) ::
          Payload((1 until 2000).map(Tuple(_)).toArray) ::
          EndSending() ::
          Nil
      sendMessages(sender, worker, events)
      Thread.sleep(2000)
      sendMessage(sender, worker, AddDownStream(policy))
      receiveMessagesUntil(receiver, EndSending())
    }

    "remove a link after processing" in {
      val sender = dummySender
      val receiver = createTestProbe[AmberInputMessage]()
      val (worker, id) = mkWorker(new MinusOne(0))
      val policy = new OneToOne(dummyWorkerIdentifier, 1, receiver.ref)
      val events: List[AmberEvent] =
        SetExpectedEndFlags(Map(id.layerIDString -> Set(id)))::
          StartSending(id) ::
          Payload((1 until 2000).map(Tuple(_)).toArray) ::
          EndSending() ::
          Nil
      sendMessage(sender, worker, AddDownStream(policy))
      sendMessages(sender, worker, events)
      Thread.sleep(2000)
      receiveMessagesUntil(receiver, EndSending())
      sendMessage(sender, worker, RemoveDownStream(policy.receiverIdentifier))
      receiver.expectNoMessage()
    }

    "remove a link during processing" in {
      val sender = dummySender
      val receiver = createTestProbe[AmberInputMessage]()
      val (worker, id) = mkWorker(new MinusOne(0))
      val policy = new OneToOne(dummyWorkerIdentifier, 1, receiver.ref)
      val events: List[AmberEvent]  = StartSending(id) :: Payload((1 until 2000).map(Tuple(_)).toArray) :: EndSending() :: Nil
      sendMessage(sender, worker, AddDownStream(policy))
      sendMessages(sender, worker, events)
      Thread.sleep(60)
      sendMessage(sender, worker, RemoveDownStream(policy.receiverIdentifier))
      receiveMessagesUntil(receiver, EndSending())
    }

    "add/remove a link during processing" in {
      val sender = dummySender
      val receiver = createTestProbe[AmberInputMessage]()
      val (worker, id) = mkWorker(new MinusOne(0))
      val policy = new OneToOne(dummyWorkerIdentifier, 1, receiver.ref)
      val events: List[AmberEvent]  = StartSending(id) :: Payload((1 until 20000).map(Tuple(_)).toArray) :: EndSending() :: Nil
      sendMessages(sender, worker, events)
      Thread.sleep(20)
      sendMessage(sender, worker, AddDownStream(policy))
      Thread.sleep(20)
      sendMessage(sender, worker, RemoveDownStream(policy.receiverIdentifier))
      receiveMessagesUntil(receiver, EndSending())
    }

  }
}

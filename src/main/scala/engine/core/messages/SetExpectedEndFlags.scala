package engine.core.messages

import akka.actor.typed.ActorRef
import engine.common.WorkerIdentifier
import engine.core.worker.{DirectControlEventForWorker, RoundTripControlEventForWorker, Worker}
import engine.core.worker.ControlPriority._

final case class SetExpectedEndFlags(expected: Map[String,Set[WorkerIdentifier]]) extends DirectControlEventForWorker {

  override val pausePriority: Int = Internal
  override val resumePriority: Int = Internal

  override def onArrive(receiver: Worker): Unit = {
    receiver.processSupport.expectedEndFlags = expected
  }
}

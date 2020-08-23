package engine.core.messages

import akka.actor.typed.ActorRef
import engine.core.data.send.policy.DataTransferPolicy
import engine.core.worker.ControlPriority.Internal
import engine.core.worker.{DirectControlEventForWorker, RoundTripControlEventForWorker, Worker}

final case class AddDownStream(policy:DataTransferPolicy) extends DirectControlEventForWorker {

  override val pausePriority: Int = Internal
  override val resumePriority: Int = Internal

  override def onArrive(receiver: Worker): Unit = {
    receiver.sendLayer.addDataTransferPolicy(policy)
  }
}

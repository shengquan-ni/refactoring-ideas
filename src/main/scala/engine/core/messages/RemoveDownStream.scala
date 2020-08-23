package engine.core.messages

import akka.actor.typed.ActorRef
import engine.common.AmberIdentifier
import engine.core.worker.ControlPriority.Internal
import engine.core.worker.{DirectControlEventForWorker, Worker}

final case class RemoveDownStream(id:AmberIdentifier) extends DirectControlEventForWorker {

  override val pausePriority: Int = Internal
  override val resumePriority: Int = Internal

  override def onArrive(receiver: Worker): Unit = {
    receiver.sendLayer.removeDataTransferPolicy(id)
  }
}

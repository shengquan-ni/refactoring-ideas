package engine.core.messages

import akka.actor.typed.ActorRef
import engine.core.worker.{DirectControlEventForWorker, RoundTripControlEventForWorker, Worker}

final case class Resume(override val resumePriority:Int) extends DirectControlEventForWorker {
  override def onArrive(receiver: Worker): Unit = {
    //do nothing
  }
}

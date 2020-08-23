package engine.core.messages

import akka.actor.typed.ActorRef
import engine.core.worker.{DirectControlEventForWorker, RoundTripControlEventForWorker, Worker}

final case class Pause(override val pausePriority:Int) extends DirectControlEventForWorker {

  override def onArrive(receiver: Worker): Unit = {
    //do nothing
  }
}

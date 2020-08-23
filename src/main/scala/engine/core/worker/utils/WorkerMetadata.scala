package engine.core.worker.utils

import akka.actor.typed.ActorRef
import engine.core.messages.AmberInputMessage

trait WorkerMetadata {
  val self: ActorRef[AmberInputMessage]
  val localManager: ActorRef[AmberInputMessage]
  val controller: ActorRef[AmberInputMessage]
}

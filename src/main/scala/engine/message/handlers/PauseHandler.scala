package engine.message.handlers

import PauseHandler.Pause
import engine.core.control.promise.{ PromiseBody, PromiseCompleted, PromiseHandler, PromiseManager }
import engine.core.worker.utils.PauseSupport

object PauseHandler {
  final case class Pause(level: Int) extends PromiseBody[PromiseCompleted]

}

trait WorkerPauseHandler extends PromiseHandler {
  this: PromiseManager with PauseSupport =>

  registerHandler { case Pause(level) =>
    // pause on worker actor
    if (trySetOuterPauseLevel(level)) {
      waitDpThreadToPause()
    }
    returning()
  }
}

trait ControllerPauseHandler extends PromiseHandler {
  this: PromiseManager =>

  registerHandler { case Pause(level) =>
    // pause all worker with the given pause level
    returning()
  }
}

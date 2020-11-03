package engine.message.handlers

import ResumeHandler.Resume
import engine.core.control.promise.{ PromiseBody, PromiseCompleted, PromiseHandler, PromiseManager }
import engine.core.worker.CoreProcessingUnit
import engine.core.worker.utils.PauseSupport

object ResumeHandler {
  final case class Resume(level: Int) extends PromiseBody[PromiseCompleted]
}

trait WorkerResumeHandler extends PromiseHandler {
  this: PromiseManager with PauseSupport with CoreProcessingUnit =>

  registerHandler { case Resume(level) =>
    // resume on worker
    if (tryReleaseOuterPauseLevel(level)) {
      tryActivate()
    }
  }

}

trait ControllerResumeHandler extends PromiseHandler {
  this: PromiseManager =>

  registerHandler { case Resume(level) =>
    // resume all worker with the given resume level
    returning()
  }
}

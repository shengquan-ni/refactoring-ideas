package engine.message.handlers

import InternalExceptionHandler.InternalException
import engine.core.control.promise.{ PromiseBody, PromiseCompleted, PromiseHandler, PromiseManager }

object InternalExceptionHandler {
  final case class InternalException(exception: Throwable) extends PromiseBody[PromiseCompleted]
}

trait InternalExceptionHandler extends PromiseHandler {
  this: PromiseManager =>

  registerHandler { case InternalException(exception) =>
    // pause all workers with PauseLevel.User -> yield control to user side
    // report exceptions to user side
    // return to the sender actor
    returning()
  }
}

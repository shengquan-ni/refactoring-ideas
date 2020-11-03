package engine.message.handlers

import engine.core.control.promise.{ PromiseBody, PromiseCompleted }

object BreakpointHandler {
  final case class BreakpointTriggered() extends PromiseBody[PromiseCompleted]
}

class BreakpointHandler {}

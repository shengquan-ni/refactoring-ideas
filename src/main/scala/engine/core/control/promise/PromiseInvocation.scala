package engine.core.control.promise

import engine.core.control.ControlMessage

case class PromiseInvocation(context:PromiseContext, call: ControlMessage[_]) extends PromiseEvent

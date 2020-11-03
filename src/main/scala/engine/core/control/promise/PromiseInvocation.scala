package engine.core.control.promise

case class PromiseInvocation(context: PromiseContext, call: PromiseBody[_]) extends PromiseEvent

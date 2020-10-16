package engine.core.control.promise

case class PromiseInvocation(context:PromiseContext, call: InternalPromise[_]) extends PromiseEvent

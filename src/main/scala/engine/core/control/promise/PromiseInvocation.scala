package engine.core.control.promise

case class PromiseInvocation(context:PromiseContext, call: AmberPromise[_]) extends PromiseEvent

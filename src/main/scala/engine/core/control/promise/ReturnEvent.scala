package engine.core.control.promise

case class ReturnEvent(context: PromiseContext, returnValue:Any) extends PromiseEvent

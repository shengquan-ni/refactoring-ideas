package engine.core.control.promise.utils

import engine.common.identifier.AmberIdentifier
import engine.core.control.promise.utils.NoReturnHandler.{NoReturn, NoReturnInvoker, NoReturnInvoker2}
import engine.core.control.promise.{InternalPromise, PromiseHandler, PromiseManager, SynchronizedInvocation, VoidInternalPromise}

object NoReturnHandler{

  final case class NoReturnInvoker(x:AmberIdentifier) extends VoidInternalPromise with SynchronizedInvocation

  final case class NoReturnInvoker2(x:AmberIdentifier) extends InternalPromise[String] with SynchronizedInvocation

  final case class NoReturn(x:String) extends VoidInternalPromise
}



trait NoReturnHandler extends PromiseHandler {
  this: PromiseManager =>

  registerHandler{
    case NoReturnInvoker2(x) =>
      schedule(NoReturn("from NoReturnInvoker2"),x)
      returning(1)
    case NoReturnInvoker(x) =>
      schedule(NoReturn("from NoReturnInvoker"),x)
    case NoReturn(x) =>
      println(s"received $x!")
  }

}

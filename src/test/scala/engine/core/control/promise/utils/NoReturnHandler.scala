package engine.core.control.promise.utils

import engine.common.identifier.AmberIdentifier
import engine.core.control.promise.utils.NoReturnHandler.{NoReturn, NoReturnInvoker}
import engine.core.control.promise.{AmberPromise, PromiseManager}

object NoReturnHandler{

  final case class NoReturnInvoker(x:AmberIdentifier) extends AmberPromise[Nothing]

  final case class NoReturn() extends AmberPromise[Nothing]
}



trait NoReturnHandler extends PromiseTesterControlHandler {
  this: PromiseManager =>

  registerHandler{
    case NoReturnInvoker(x) =>
      schedule(NoReturn(),x)
    case NoReturn() =>
      println("received!")
  }

}

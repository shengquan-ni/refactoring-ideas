package engine.core.control.promise.utils

import engine.common.identifier.Identifier
import engine.core.control.ControlMessage
import engine.core.control.promise.utils.ChainHandler.Chain
import engine.core.control.promise.utils.CollectHandler.Collect
import engine.core.control.promise.utils.RecursionHandler.Recursion
import engine.core.control.promise.utils.SubPromiseHandler.PromiseInvoker
import engine.core.control.promise.{InternalPromise, PromiseHandler, PromiseManager, SynchronizedInvocation}


object SubPromiseHandler{
  case class PromiseInvoker(seq:Seq[Identifier]) extends ControlMessage[String]
}


trait SubPromiseHandler extends PromiseHandler {
  this: PromiseManager =>

  registerHandler{
    case PromiseInvoker(seq) =>
      schedule(Chain(seq)).map{
        x:Identifier =>
          schedule(Recursion(1), x).map{
            ret:String =>
              returning(ret)
          }
      }
      schedule(Collect(seq.take(3)))
  }
}

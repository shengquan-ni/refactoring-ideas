package engine.core.control.promise.utils

import engine.common.identifier.AmberIdentifier
import engine.core.control.promise.utils.ChainHandler.Chain
import engine.core.control.promise.utils.CollectHandler.Collect
import engine.core.control.promise.utils.RecursionHandler.Recursion
import engine.core.control.promise.utils.SubPromiseHandler.PromiseInvoker
import engine.core.control.promise.{AmberPromise, PromiseHandler, PromiseManager, SynchronizedExecution}

object SubPromiseHandler{
  case class PromiseInvoker(seq:Seq[AmberIdentifier]) extends AmberPromise[String]
}


trait SubPromiseHandler extends PromiseHandler {
  this: PromiseManager =>

  registerHandler{
    case PromiseInvoker(seq) =>
      after(schedule(Chain(seq))){
        x:AmberIdentifier =>
          after(schedule(Recursion(1), x)){
            ret:String =>
              returning(ret)
          }
      }
      schedule(Collect(seq.take(3)))
  }
}

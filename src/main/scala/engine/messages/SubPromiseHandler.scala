package engine.messages

import engine.common.AmberIdentifier
import engine.core.{ControlScheduler, CoreProcessingUnit}
import engine.messages.ChainHandler.Chain
import engine.messages.CollectHandler.Collect
import engine.messages.RecursionHandler.Recursion
import engine.messages.SubPromiseHandler.PromiseInvoker

object SubPromiseHandler{
  case class PromiseInvoker(seq:Seq[AmberIdentifier]) extends AmberPromise
}


trait SubPromiseHandler extends WorkerControlHandler {
  this:CoreProcessingUnit with ControlScheduler =>

  registerHandler{
    case PromiseInvoker(seq) =>
      localPromise(Chain(seq)).onComplete{
        x:AmberIdentifier =>
          remotePromise(x, Recursion(1)).onComplete{
            ret:String =>
              returning(ret)
          }
      }
      localPromise(Collect(seq.take(3)))
  }
}

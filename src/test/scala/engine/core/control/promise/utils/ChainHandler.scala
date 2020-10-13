package engine.core.control.promise.utils

import engine.common.identifier.{ActorIdentifier, AmberIdentifier}
import engine.core.control.promise.utils.ChainHandler.Chain
import engine.core.control.promise.utils.NestedHandler.Pass
import engine.core.control.promise.{AmberPromise, PromiseHandler, PromiseManager, SynchronizedExecution}
import engine.core.worker.CoreProcessingUnit


object ChainHandler{
  case class Chain(nexts:Seq[AmberIdentifier]) extends AmberPromise[AmberIdentifier] with SynchronizedExecution
}


trait ChainHandler extends PromiseHandler {
  this: PromiseManager with DummyStateComponent=>

  registerHandler{
    case Chain(nexts) =>
      println(s"chained $getLocalIdentifier")
      if(nexts.isEmpty){
        returning(getLocalIdentifier)
      }else{
        after(schedule(Chain(nexts.drop(1)),nexts.head)){
          x =>
            println(s"chain returns from $x")
            returning(x)
        }
      }
  }

}

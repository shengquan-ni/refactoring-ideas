package engine.core.control.promise.utils

import engine.common.identifier.{ActorIdentifier, Identifier}
import engine.core.control.ControlMessage
import engine.core.control.promise.utils.ChainHandler.Chain
import engine.core.control.promise.utils.NestedHandler.Pass
import engine.core.control.promise.{InternalPromise, PromiseHandler, PromiseManager, SynchronizedInvocation}
import engine.core.worker.CoreProcessingUnit


object ChainHandler{
  case class Chain(nexts:Seq[Identifier]) extends ControlMessage[Identifier] with SynchronizedInvocation
}


trait ChainHandler extends PromiseHandler {
  this: PromiseManager with DummyState=>

  registerHandler{
    case Chain(nexts) =>
      println(s"chained $getLocalIdentifier")
      if(nexts.isEmpty){
        returning(getLocalIdentifier)
      }else{
        schedule(Chain(nexts.drop(1)),nexts.head).map{
          x =>
            println(s"chain returns from $x")
            returning(x)
        }
      }
  }

}

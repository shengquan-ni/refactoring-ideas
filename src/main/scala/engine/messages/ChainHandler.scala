package engine.messages

import engine.common.AmberIdentifier
import engine.core.{ControlScheduler, CoreProcessingUnit}
import engine.messages.ChainHandler.Chain

object ChainHandler{
  case class Chain(nexts:Seq[AmberIdentifier]) extends AmberPromise
}


trait ChainHandler extends WorkerControlHandler {
  this: CoreProcessingUnit with ControlScheduler =>

  registerHandler{
    case Chain(nexts) =>
      println(s"chained $getLocalIdentifier")
      if(nexts.isEmpty){
        returning(getLocalIdentifier)
      }else{
        remotePromise(nexts.head, Chain(nexts.drop(1))).onComplete{
          x:AmberIdentifier =>
            println(s"chain returns from $x")
            returning(getLocalIdentifier)
        }
      }
  }
}

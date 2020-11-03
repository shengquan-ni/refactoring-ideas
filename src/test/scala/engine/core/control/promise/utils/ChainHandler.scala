package engine.core.control.promise.utils

import engine.common.identifier.Identifier
import engine.core.control.promise.utils.ChainHandler.Chain
import engine.core.control.promise.{
  PromiseBody,
  PromiseHandler,
  PromiseManager,
  SynchronizedInvocation,
}

object ChainHandler {
  case class Chain(nexts: Seq[Identifier])
    extends PromiseBody[Identifier]
    with SynchronizedInvocation
}

trait ChainHandler extends PromiseHandler {
  this: PromiseManager =>

  registerHandler { case Chain(nexts) =>
    println(s"chained $getLocalIdentifier")
    if (nexts.isEmpty) {
      returning(getLocalIdentifier)
    } else {
      schedule(Chain(nexts.drop(1)), nexts.head).map { x =>
        println(s"chain returns from $x")
        returning(x)
      }
    }
  }

}

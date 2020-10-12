package engine.core.control.promise.utils

import engine.common.identifier.AmberIdentifier
import engine.core.control.promise.utils.BlockHandler.{Block, NonBlock, Sleep}
import engine.core.control.promise.utils.NestedHandler.Pass
import engine.core.control.promise.{AmberPromise, PromiseHandler, PromiseManager}

object BlockHandler{
  case class Block(i:Int, to:AmberIdentifier) extends AmberPromise[Int]
  case class Sleep(i:Int) extends AmberPromise[Int]
  case class NonBlock(i:Int) extends AmberPromise[Int]
}


trait BlockHandler extends PromiseHandler {
  this: PromiseManager with DummyStateComponent =>

  // Enforce something similar to SS2PL -- may cause Global DeadLock!
  registerPrerequisite{
    case Block(i, to) =>
      tryLock(state)
      //true
  }


  registerHandler{
    case Block(i, to) =>
      println("Start to block")
      state.lockedVariable = i
      after(schedule(Sleep(i),to)){
        ret =>
          println(s"blocks for $ret ms")
          println(s"lockedVariable = ${state.lockedVariable}")
          returning(state.lockedVariable)
      }

    case Sleep(i) =>
      Thread.sleep(i)
      returning(i)

    case NonBlock(i) =>
      println(s"process nonblocking promise with arg = $i")
      returning(i)
  }

  // Enforce something similar to SS2PL
  registerCleanup{
    case Block(i, to) =>
      unlock(state)
  }

}

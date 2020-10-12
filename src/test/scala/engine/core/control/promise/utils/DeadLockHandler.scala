package engine.core.control.promise.utils

import engine.core.control.promise.utils.BlockHandler.Block
import engine.core.control.promise.utils.DeadLockHandler.DeadLock
import engine.core.control.promise.{AmberPromise, PromiseHandler, PromiseManager}

object DeadLockHandler{
  case class DeadLock(i:Int) extends AmberPromise[Int]
}


trait DeadLockHandler extends PromiseHandler {
  this: PromiseManager with DummyStateComponent =>

  registerPrerequisite{
    case DeadLock(i) =>
      tryLock(state)
  }

  registerHandler{
    case DeadLock(i) =>
      println("enter deadlock")
      after(schedule(Block(i, getLocalIdentifier))){
        ret =>
          returning(ret)
      }
  }
  registerCleanup{
    case DeadLock(i) =>
      unlock(state)
  }

}

package engine.messages

import engine.common.AmberIdentifier
import engine.core.ControlScheduler

class NestedPromise(to:AmberIdentifier) extends AmberPromise {
  override def invoke()(implicit context: PromiseContext, scheduler: ControlScheduler): Unit = {
    localPromise(Pass("Hello")).onComplete{
      ret:String =>
        localPromise(Pass(" ")).onComplete{
          ret2:String =>
            localPromise(Pass("World!")).onComplete{
              ret3:String =>
                remotePromise(to,new PrintString(ret+ret2+ret3))
                returning(ret+ret2+ret3)
            }
        }
    }
  }
}

class PrintString(str:String) extends AmberPromise{
  override def invoke()(implicit context:PromiseContext, scheduler:ControlScheduler): Unit = {
    println(s"$str")
  }
}


package engine.messages

import engine.core.{ControlScheduler, CoreProcessingUnit}
import engine.messages.NestedHandler.{Nested, Pass}

object NestedHandler{
  case class Nested(k:Int) extends AmberPromise

  case class Pass[T](value:T) extends AmberPromise
}


trait NestedHandler extends WorkerControlHandler {
  this: CoreProcessingUnit with ControlScheduler =>

  registerHandler{
    case Nested(k) =>
      processedCount += k
      localPromise(Pass("Hello")).onComplete{
        ret:String =>
          localPromise(Pass(" ")).onComplete{
            ret2:String =>
              localPromise(Pass("World!")).onComplete{
                ret3:String =>
                  println(ret+ret2+ret3)
                  returning(ret+ret2+ret3)
              }
          }
      }
    case Pass(a) =>
      returning(a)
  }
}

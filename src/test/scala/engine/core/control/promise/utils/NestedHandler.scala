package engine.core.control.promise.utils

import engine.core.control.promise.utils.NestedHandler.{Nested, Pass}
import engine.core.control.promise.{AmberPromise, PromiseManager}

object NestedHandler{
  case class Nested(k:Int) extends AmberPromise[String]

  case class Pass[T](value:T) extends AmberPromise[T]
}


trait NestedHandler extends PromiseTesterControlHandler {
  this: PromiseManager =>

  registerHandler{
    case Nested(k) =>
      after(schedule(Pass("Hello"))){
        ret:String =>
          after(schedule(Pass(" "))){
            ret2:String =>
              after(schedule(Pass("World!"))){
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

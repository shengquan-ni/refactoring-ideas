package engine.core.control.promise.utils

import engine.core.control.promise.utils.NestedHandler.{Nested, Pass}
import engine.core.control.promise.{InternalPromise, PromiseHandler, PromiseManager}

object NestedHandler{
  case class Nested(k:Int) extends InternalPromise[String]

  case class Pass[T](value:T) extends InternalPromise[T]
}


trait NestedHandler extends PromiseHandler {
  this: PromiseManager with DummyStateComponent =>

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

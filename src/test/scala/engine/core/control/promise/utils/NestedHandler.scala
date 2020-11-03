package engine.core.control.promise.utils

import engine.core.control.promise.utils.NestedHandler.{ Nested, Pass }
import engine.core.control.promise.{ InternalPromise, PromiseBody, PromiseHandler, PromiseManager }

import scala.reflect.runtime.universe._

object NestedHandler {
  case class Nested(k: Int) extends PromiseBody[String]

  case class Pass[T: TypeTag](value: T) extends PromiseBody[T]
}

trait NestedHandler extends PromiseHandler {
  this: PromiseManager with DummyState =>

  registerHandler {
    case Nested(k) =>
      schedule(Pass("Hello")).map { ret: String =>
        schedule(Pass(" ")).map { ret2: String =>
          schedule(Pass("World!")).map { ret3: String =>
            println(ret + ret2 + ret3)
            returning(ret + ret2 + ret3)
          }
        }
      }
    case Pass(a) =>
      returning(a)
  }
}

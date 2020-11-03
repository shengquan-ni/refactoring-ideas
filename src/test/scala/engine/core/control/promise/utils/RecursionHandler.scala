package engine.core.control.promise.utils

import engine.core.control.promise.utils.RecursionHandler.Recursion
import engine.core.control.promise.{
  InternalPromise,
  PromiseBody,
  PromiseHandler,
  PromiseManager,
  SyntaxChecker,
}

object RecursionHandler {
  case class Recursion(i: Int) extends PromiseBody[String]
}

trait RecursionHandler extends PromiseHandler {
  this: PromiseManager =>

  registerHandler { case Recursion(i) =>
    if (i < 5) {
      println(i)
      schedule(Recursion(i + 1)).map { res =>
        println(res)
        returning(i.toString)
      }
    } else {
      returning(i.toString)
    }
  }

}

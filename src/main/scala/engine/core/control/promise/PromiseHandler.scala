package engine.core.control.promise

import scala.language.experimental.macros

trait PromiseHandler {
  this: PromiseManager =>

  def registerHandler(eventHandler: PartialFunction[PromiseBody[_], Unit]): Unit =
    macro SyntaxChecker.handlerImpl

}

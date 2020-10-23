package engine.core.control.promise

import engine.core.control.ControlMessage

import scala.language.experimental.macros


trait PromiseHandler {
  this: PromiseManager =>

  def registerHandler(eventHandler:PartialFunction[ControlMessage[_], Unit]): Unit = macro SyntaxChecker.handlerImpl

}

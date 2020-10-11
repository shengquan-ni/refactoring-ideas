package engine.core.control.promise.utils

import engine.core.control.promise.{AmberPromise, PromiseManager}

trait PromiseTesterControlHandler {
  this: PromiseManager =>

  def registerHandler(eventHandler:PartialFunction[AmberPromise[_], Unit]): Unit = {
    promiseHandler = eventHandler orElse promiseHandler
  }

}

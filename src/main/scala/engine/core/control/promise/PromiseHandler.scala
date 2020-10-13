package engine.core.control.promise

trait PromiseHandler {
  this: PromiseManager =>

  def registerHandler(eventHandler:PartialFunction[AmberPromise[_], Unit]): Unit = {
    promiseHandler = eventHandler orElse promiseHandler
  }

}

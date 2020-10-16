package engine.core.control.promise

trait PromiseHandler {
  this: PromiseManager =>

  def registerHandler(eventHandler:PartialFunction[InternalPromise[_], Unit]): Unit = {
    promiseHandler = eventHandler orElse promiseHandler
  }

}

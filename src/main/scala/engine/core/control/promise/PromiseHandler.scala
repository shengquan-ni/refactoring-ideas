package engine.core.control.promise

trait PromiseHandler {
  this: PromiseManager =>


  def registerPrerequisite(prerequisite:PartialFunction[AmberPromise[_], Boolean]): Unit ={
    prerequisiteHandler = prerequisite orElse prerequisiteHandler
  }

  def registerHandler(eventHandler:PartialFunction[AmberPromise[_], Unit]): Unit = {
    promiseHandler = eventHandler orElse promiseHandler
  }

  def registerCleanup(cleanup:PartialFunction[AmberPromise[_], Unit]):Unit = {
    cleanupHandler = cleanup orElse cleanupHandler
  }

}

package engine.messages

import engine.core.{ControlScheduler, CoreProcessingUnit}

trait WorkerControlHandler {
  this: CoreProcessingUnit with ControlScheduler =>

  def registerHandler(eventHandler:PartialFunction[AmberPromise, Unit]): Unit = {
    promiseHandler = eventHandler orElse promiseHandler
  }

}

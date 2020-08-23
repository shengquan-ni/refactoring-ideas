package engine.core.worker

import engine.core.messages.{AmberEvent, BackwardControlEvent, CollectiveControlEvent, DirectControlEvent, RoundTripControlEvent}
import engine.core.worker.ControlPriority.No

trait WorkerControlLogic{
  val pausePriority:Int = No
  val resumePriority:Int = No
}

trait CollectiveControlEventForWorker extends WorkerControlLogic with CollectiveControlEvent[Worker]

trait RoundTripControlEventForWorker extends WorkerControlLogic with RoundTripControlEvent[Worker]

trait DirectControlEventForWorker extends WorkerControlLogic with DirectControlEvent[Worker]

trait BackwardControlEventForWorker extends WorkerControlLogic with BackwardControlEvent[Worker]

trait DataEvent extends AmberEvent{
  def onArrive(receiver: Worker)
}


trait DataProcessingEvent extends AmberEvent{
}

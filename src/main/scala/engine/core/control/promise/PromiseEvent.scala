package engine.core.control.promise

import engine.event.ControlEvent

trait PromiseEvent extends ControlEvent {
  val context: PromiseContext
}

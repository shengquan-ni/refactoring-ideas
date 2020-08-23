package engine.core.messages

import akka.actor.typed.ActorRef
import engine.core.worker.ControlPriority.No

//base type of all events
trait AmberEvent

trait ControlEvent extends AmberEvent{
}

trait CollectiveControlEvent[T] extends ControlEvent{

  val id:Long

  def onDispatch(receiver: T)

  def onCollect(receiver: T, reply:BackwardControlEvent[T])

  def isResolved:Boolean
}


trait RoundTripControlEvent[T] extends ControlEvent{
  val replyTo: ActorRef[AmberInputMessage]
  def onArrive(receiver: T)
}

trait DirectControlEvent[T] extends ControlEvent{
  def onArrive(receiver: T)
}


trait BackwardControlEvent[T] extends ControlEvent{

  val id:Long

}









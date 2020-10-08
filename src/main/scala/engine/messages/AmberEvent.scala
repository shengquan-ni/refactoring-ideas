package engine.messages

import java.util.concurrent.Callable

import akka.dispatch.Futures
import engine.common.{AmberIdentifier, AmberRemoteIdentifier, WorkflowIdentifier}
import engine.core.ControlScheduler

import scala.collection.mutable
import scala.concurrent.Future


//base type of all events
trait AmberEvent extends Serializable

trait ControlEvent extends AmberEvent{
  val context:PromiseContext
}

trait DataEvent extends AmberEvent

case class PromiseContext(sender: AmberIdentifier, id:Long)

case class AmberFuture(id:Long, scheduler: ControlScheduler){
  def onComplete[T](f: T => Unit): Unit ={
    scheduler.callAfterComplete(id, f)
  }
}


case class GroupedFutures(amberFutures:AmberFuture*)(implicit scheduler: ControlScheduler){
  def onComplete[T](f:Seq[T] => Unit):Unit ={
    scheduler.callAfterGroupComplete(amberFutures.map(_.id),f)
  }
}

trait AmberPromise{
  def invoke()(implicit context:PromiseContext, scheduler:ControlScheduler):Unit

  final def remotePromise(to:AmberIdentifier, cmd:AmberPromise)(implicit context:PromiseContext, scheduler:ControlScheduler): AmberFuture = scheduler.remotePromise(to, cmd)

  final def localPromise(cmd:AmberPromise)(implicit context:PromiseContext, scheduler:ControlScheduler): AmberFuture = scheduler.localPromise(cmd)

  final def returning(value: Any)(implicit context:PromiseContext, scheduler:ControlScheduler): Unit = scheduler.returning(value)
}


case class PromiseEvent(context:PromiseContext, call: AmberPromise) extends ControlEvent

case class AfterCompletion[T](f: T => Unit){
  def invoke(scheduler: ControlScheduler, returnValue: Any): Unit = {
    f(returnValue.asInstanceOf[T])
  }
}


case class AfterGroupCompletion[T](id:Seq[Long], f: Seq[T] => Unit){

  val returnValues:mutable.ArrayBuffer[T] = mutable.ArrayBuffer[T]()
  val expectedIds:mutable.HashSet[Long] = mutable.HashSet[Long](id:_*)

  def takeReturnValue(returnEvent: ReturnEvent): Boolean ={
    if(expectedIds.contains(returnEvent.context.id)){
      expectedIds.remove(returnEvent.context.id)
      returnValues.append(returnEvent.returnValue.asInstanceOf[T])
    }
    expectedIds.isEmpty
  }

  def invoke(scheduler: ControlScheduler): Unit ={
    f(returnValues.toSeq)
  }
}

case class ReturnEvent(context: PromiseContext, returnValue:Any) extends ControlEvent











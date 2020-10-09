package engine.core

import engine.common.AmberIdentifier
import engine.messages.{AfterCompletion, AfterGroupCompletion, AmberFuture, AmberPromise, ControlEvent, PromiseContext, PromiseEvent, ReturnEvent}

import scala.collection.mutable

trait ControlScheduler {
  this:AmberActor with ControlOutputChannel =>

  implicit private val scheduler: ControlScheduler = this

  private val afterComplete = mutable.LongMap[AfterCompletion[_]]()
  private val afterGroupComplete = mutable.HashSet[AfterGroupCompletion[_]]()
  private var ID = 0L
  private var controlContext:PromiseContext = _

  var promiseHandler:PartialFunction[AmberPromise, Unit] = {
    case promise =>
      log.info(s"discarding $promise")
  }

  def schedule(event: ControlEvent): Unit = {
    event match{
      case ret:ReturnEvent =>
        if(afterComplete.contains(ret.context.id)){
          controlContext = afterComplete(ret.context.id).context
          afterComplete(ret.context.id).invoke(this,ret.returnValue)
          afterComplete.remove(ret.context.id)
        }
        for(i <- afterGroupComplete){
          if(i.takeReturnValue(ret)){
            controlContext = i.context
            i.invoke(this)
            afterGroupComplete.remove(i)
          }
        }
      case remoteCall: PromiseEvent =>
        controlContext = event.context
        promiseHandler(remoteCall.call)
    }
  }

  def remotePromise(to:AmberIdentifier, cmd:AmberPromise):AmberFuture = {
    val evt = PromiseEvent(PromiseContext(amberID,ID),cmd)
    sendTo(to, evt)
    val handle = AmberFuture(ID,this)
    ID += 1
    handle
  }

  def localPromise(cmd:AmberPromise):AmberFuture = {
    remotePromise(amberID,cmd)
  }

  def returning(value:Any): Unit ={
    sendTo(controlContext.sender, ReturnEvent(controlContext,value))
  }


  def callAfterComplete[T](id:Long, cmd:T => Unit): Unit = {
    afterComplete(id) = AfterCompletion(controlContext, cmd)
  }

  def callAfterGroupComplete[T](ids:Seq[Long], cmd: Seq[T] => Unit):Unit = {
    afterGroupComplete.add(AfterGroupCompletion(controlContext, ids, cmd))
  }

  def getLocalIdentifier:AmberIdentifier = amberID

}

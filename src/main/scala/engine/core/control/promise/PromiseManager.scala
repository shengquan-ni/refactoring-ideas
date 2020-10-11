package engine.core.control.promise

import engine.common.identifier.AmberIdentifier
import engine.core.AmberActor
import engine.core.control.ControlOutputChannel

import scala.collection.mutable

trait PromiseManager {
  this: AmberActor with ControlOutputChannel =>

  private val afterComplete = mutable.LongMap[AfterCompletion[_]]()
  private val afterGroupComplete = mutable.HashSet[AfterGroupCompletion[_]]()
  private var ID = 0L
  private var promiseContext:PromiseContext = _

  var promiseHandler:PartialFunction[AmberPromise[_], Unit] = {
    case promise =>
      log.info(s"discarding $promise")
  }

  def scheduleInternal(event: PromiseEvent): Unit = {
    event match{
      case ret:ReturnEvent =>
        if(afterComplete.contains(ret.context.id)){
          promiseContext = afterComplete(ret.context.id).context
          afterComplete(ret.context.id).invoke(ret.returnValue)
          afterComplete.remove(ret.context.id)
        }
        for(i <- afterGroupComplete){
          if(i.takeReturnValue(ret)){
            promiseContext = i.context
            i.invoke()
            afterGroupComplete.remove(i)
          }
        }
      case invocation: PromiseInvocation =>
        promiseContext = event.context
        promiseHandler(invocation.call)
    }
  }

  def schedule[T](cmd:AmberPromise[T], on:AmberIdentifier = amberID):AmberFuture[T] = {
    val evt = PromiseInvocation(PromiseContext(amberID,ID),cmd)
    sendTo(on, evt)
    val handle = AmberFuture[T](ID)
    ID += 1
    handle
  }

  def schedule(cmd:AmberPromise[Nothing]):Unit = {
    val evt = PromiseInvocation(PromiseContext(amberID,ID),cmd)
    sendTo(amberID, evt)
  }

  def schedule(cmd:AmberPromise[Nothing], on: AmberIdentifier):Unit = {
    val evt = PromiseInvocation(PromiseContext(amberID,ID),cmd)
    sendTo(on, evt)
  }

  def after[T](future: AmberFuture[T])(f:T => Unit): Unit ={
    afterComplete(future.id) = AfterCompletion(promiseContext, f)
  }

  def after[T](futures:AmberFuture[T]*)(f:Seq[T] => Unit):Unit = {
    afterGroupComplete.add(AfterGroupCompletion(promiseContext, futures.map(_.id), f))
  }

  def returning(value:Any): Unit ={
    // returning should be used at most once per context
    if(promiseContext != null){
      sendTo(promiseContext.sender, ReturnEvent(promiseContext,value))
      promiseContext = null
    }
  }

  def getLocalIdentifier:AmberIdentifier = amberID

}

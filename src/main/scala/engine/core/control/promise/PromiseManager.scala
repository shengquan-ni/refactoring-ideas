package engine.core.control.promise

import engine.common.identifier.AmberIdentifier
import engine.core.AmberActor
import engine.core.control.ControlOutputChannel
import engine.utils.Lockable

import scala.collection.mutable

trait PromiseManager {
  this: AmberActor with ControlOutputChannel =>

  private val afterComplete = mutable.LongMap[AfterCompletion[_]]()
  private val afterGroupComplete = mutable.HashSet[AfterGroupCompletion[_]]()
  private var ID = 0L
  private var promiseContext:PromiseContext = _
  private val stashedInvocations = mutable.HashSet[PromiseInvocation]()
  private val unResolvedPromiseMap = mutable.AnyRefMap[PromiseContext, AmberPromise[_]]()

  var promiseHandler:PartialFunction[AmberPromise[_], Unit] = {
    case promise =>
      log.info(s"discarding $promise")
  }

  var prerequisiteHandler:PartialFunction[AmberPromise[_],Boolean] = {
    case withoutPrerequisite =>
      true
  }

  var cleanupHandler:PartialFunction[AmberPromise[_],Unit] = {
    case noCleanup =>
      // do nothing
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
      case p: PromiseInvocation =>
        promiseContext = p.context
        if(prerequisiteHandler(p.call)){
          unResolvedPromiseMap(p.context) = p.call
          promiseHandler(p.call)
        }else{
          stashedInvocations.addOne(p)
        }
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
      val current = promiseContext
      sendTo(current.sender, ReturnEvent(current,value))
      cleanupHandler(unResolvedPromiseMap(current))
      unResolvedPromiseMap.remove(current)
    }
  }

  def getLocalIdentifier:AmberIdentifier = amberID


  def tryLock(states:Lockable*): Boolean ={
    if(states.exists(_.isLocked)){
      false
    }else{
      states.foreach(_.lock(promiseContext))
      true
    }
  }

  def unlock(states:Lockable*):Unit ={
    assert(states.forall(_.getOwner == promiseContext))
    states.foreach(_.unlock())
    scheduleDelayedPromises()
  }

  private def scheduleDelayedPromises(): Unit ={
    val toRemove = mutable.ArrayBuffer[PromiseInvocation]()
    stashedInvocations.foreach{
      p =>
        promiseContext = p.context
        if(prerequisiteHandler(p.call)){
          toRemove.addOne(p)
          unResolvedPromiseMap(p.context) = p.call
          promiseHandler(p.call)
        }
    }
    stashedInvocations --= toRemove
  }

}

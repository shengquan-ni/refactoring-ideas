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
  private val queuedInvocations = mutable.Queue[PromiseEvent]()
  private val ongoingSyncPromises = mutable.HashSet[PromiseContext]()
  private var syncPromiseRoot:PromiseContext = _

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
      case PromiseInvocation(ctx:RootPromiseContext, call:SynchronizedExecution) =>
        if(syncPromiseRoot == null){
          syncPromiseRoot = ctx
          promiseContext = ctx
          ongoingSyncPromises.add(promiseContext)
          promiseHandler(call)
        }else{
          queuedInvocations.enqueue(event)
        }
      case PromiseInvocation(ctx:ChildPromiseContext, call:SynchronizedExecution) =>
        if(syncPromiseRoot == null || ctx.root == syncPromiseRoot){
          syncPromiseRoot = ctx.root
          promiseContext = ctx
          ongoingSyncPromises.add(promiseContext)
          promiseHandler(call)
        }else{
          queuedInvocations.enqueue(event)
        }
      case PromiseInvocation(ctx, call) =>
        promiseContext = ctx
        promiseHandler(call)
    }
  }

  def schedule[T](cmd:AmberPromise[T], on:AmberIdentifier = amberID):AmberFuture[T] = {
    sendTo(on, PromiseInvocation(mkPromiseContext(),cmd))
    val handle = AmberFuture[T](ID)
    ID += 1
    handle
  }

  @inline
  private def mkPromiseContext():PromiseContext = {
    promiseContext match{
      case ctx:RootPromiseContext =>
        PromiseContext(amberID, ID, ctx)
      case ctx:ChildPromiseContext =>
        PromiseContext(amberID, ID, ctx.root)
    }
  }

  def schedule(cmd:AmberPromise[Nothing]):Unit = {
    sendTo(amberID, PromiseInvocation(mkPromiseContext(),cmd))
  }

  def schedule(cmd:AmberPromise[Nothing], on: AmberIdentifier):Unit = {
    sendTo(on, PromiseInvocation(mkPromiseContext(),cmd))
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
      if(ongoingSyncPromises.contains(promiseContext)) {
        ongoingSyncPromises.remove(promiseContext)
        if(ongoingSyncPromises.isEmpty){
          syncPromiseRoot = null
          if(queuedInvocations.nonEmpty){
            scheduleInternal(queuedInvocations.dequeue())
          }
        }
      }
    }
  }

  def getLocalIdentifier:AmberIdentifier = amberID

}

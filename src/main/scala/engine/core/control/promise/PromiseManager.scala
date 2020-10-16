package engine.core.control.promise

import engine.common.identifier.AmberIdentifier
import engine.core.InternalActor
import engine.core.control.ControlOutputChannel

import scala.collection.mutable

trait PromiseManager {
  this: InternalActor with ControlOutputChannel =>

  private val afterComplete = mutable.LongMap[AfterCompletion[_]]()
  private val afterGroupComplete = mutable.HashSet[AfterGroupCompletion[_]]()
  private var ID = 0L
  private var promiseContext:PromiseContext = _
  private val queuedInvocations = mutable.Queue[PromiseEvent]()
  private val ongoingSyncPromises = mutable.HashSet[PromiseContext]()
  private var syncPromiseRoot:PromiseContext = _

  var promiseHandler:PartialFunction[InternalPromise[_], Unit] = {
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
      case PromiseInvocation(ctx:RootPromiseContext, call:SynchronizedInvocation) =>
        if(syncPromiseRoot == null){
          registerSyncPromise(ctx,ctx)
          invokePromise(ctx,call)
        }else{
          queuedInvocations.enqueue(event)
        }
      case PromiseInvocation(ctx:ChildPromiseContext, call:SynchronizedInvocation) =>
        if(syncPromiseRoot == null || ctx.root == syncPromiseRoot){
          registerSyncPromise(ctx.root,ctx)
          invokePromise(ctx,call)
        }else{
          queuedInvocations.enqueue(event)
        }
      case PromiseInvocation(ctx, call) =>
        invokePromise(ctx, call)
    }
    tryInvokeNextSyncPromise()
  }

  def schedule[T](cmd:InternalPromise[T], on:AmberIdentifier = amberID):InternalFuture[T] = {
    sendTo(on, PromiseInvocation(mkPromiseContext(),cmd))
    val handle = InternalFuture[T](ID)
    ID += 1
    handle
  }

  def schedule(cmd:InternalPromise[Nothing]):Unit = {
    sendTo(amberID, PromiseInvocation(mkPromiseContext(),cmd))
  }

  def schedule(cmd:InternalPromise[Nothing], on: AmberIdentifier):Unit = {
    sendTo(on, PromiseInvocation(mkPromiseContext(),cmd))
  }

  def after[T](future: InternalFuture[T])(f:T => Unit): Unit ={
    afterComplete(future.id) = AfterCompletion(promiseContext, f)
  }

  def after[T](futures:InternalFuture[T]*)(f:Seq[T] => Unit):Unit = {
    afterGroupComplete.add(AfterGroupCompletion(promiseContext, futures.map(_.id), f))
  }

  def returning(value:Any): Unit ={
    // returning should be used at most once per context
    if(promiseContext != null){
      sendTo(promiseContext.sender, ReturnEvent(promiseContext,value))
      exitCurrentPromise()
    }
  }

  def getLocalIdentifier:AmberIdentifier = amberID

  @inline
  private def exitCurrentPromise(): Unit ={
    if(ongoingSyncPromises.contains(promiseContext)) {
      ongoingSyncPromises.remove(promiseContext)
    }
    promiseContext = null
  }

  @inline
  private def tryInvokeNextSyncPromise(): Unit ={
    if(ongoingSyncPromises.isEmpty){
      syncPromiseRoot = null
      if(queuedInvocations.nonEmpty){
        scheduleInternal(queuedInvocations.dequeue())
      }
    }
  }

  @inline
  private def registerSyncPromise(rootCtx:RootPromiseContext, ctx:PromiseContext): Unit ={
    syncPromiseRoot = rootCtx
    ongoingSyncPromises.add(ctx)
  }

  @inline
  private def invokePromise(ctx:PromiseContext, call:InternalPromise[_]): Unit ={
    promiseContext = ctx
    promiseHandler(call)
    if(call.isInstanceOf[VoidInternalPromise]){
      exitCurrentPromise()
    }
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

}

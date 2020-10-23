package engine.core.control.promise

import com.twitter.util.{Future, Promise}
import engine.common.identifier.Identifier
import engine.core.InternalActor
import engine.core.control.{ControlMessage, ControlOutputChannel}

import scala.collection.mutable

trait PromiseManager {
  this: InternalActor with ControlOutputChannel =>

  private var ID = 0L
  private val unCompletedPromises = mutable.HashMap[PromiseContext, InternalPromise[_]]()
  private val unCompletedGroupPromises = mutable.HashSet[GroupedInternalPromise[_]]()
  private var promiseContext:PromiseContext = _
  private val queuedInvocations = mutable.Queue[PromiseEvent]()
  private val ongoingSyncPromises = mutable.HashSet[PromiseContext]()
  private var syncPromiseRoot:PromiseContext = _

  protected var promiseHandler:PartialFunction[ControlMessage[_], Unit] = {
    case promise =>
      log.info(s"discarding $promise")
  }

  def consume(event: PromiseEvent): Unit = {
    event match{
      case ret:ReturnEvent =>
        if(unCompletedPromises.contains(ret.context)) {
          val p = unCompletedPromises(ret.context)
          promiseContext = p.ctx
          ret.returnValue match {
            case throwable: Throwable =>
              p.setException(throwable)
            case _ =>
              p.setValue(ret.returnValue.asInstanceOf[p.returnType])
          }
          unCompletedPromises.remove(ret.context)
        }

        for(i <- unCompletedGroupPromises){
          if(i.takeReturnValue(ret)){
            promiseContext = i.promise.ctx
            i.invoke()
            unCompletedGroupPromises.remove(i)
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
        }else {
          queuedInvocations.enqueue(event)
        }
      case p:PromiseInvocation =>
        promiseContext = p.context
        try{
          promiseHandler(p.call)
        }catch{
          case e:Throwable =>
            returning(e)
        }
    }
    tryInvokeNextSyncPromise()
  }

  def schedule[T](cmd:ControlMessage[T], on:Identifier = amberID):Promise[T] = {
    val ctx = mkPromiseContext()
    ID += 1
    sendTo(on, PromiseInvocation(ctx,cmd))
    val promise = InternalPromise[T](promiseContext)
    unCompletedPromises(ctx) = promise
    promise
  }

  def schedule[T](seq:(ControlMessage[T], Identifier)*):Promise[Seq[T]] = {
    val promise = InternalPromise[Seq[T]](promiseContext)
    if(seq.isEmpty){
      promise.setValue(Seq.empty)
    }else{
      unCompletedGroupPromises.add(GroupedInternalPromise[T](ID,ID+seq.length, promise))
      seq.foreach{
        i =>
          val ctx = mkPromiseContext()
          ID += 1
          sendTo(i._2, PromiseInvocation(ctx,i._1))
      }
    }
    promise
  }


  def returning(value:Any): Unit ={
    // returning should be used at most once per context
    if(promiseContext != null){
      sendTo(promiseContext.sender, ReturnEvent(promiseContext,value))
      exitCurrentPromise()
    }
  }

  def returning(): Unit ={
    // returning should be used at most once per context
    if(promiseContext != null){
      sendTo(promiseContext.sender, ReturnEvent(promiseContext,PromiseCompleted()))
      exitCurrentPromise()
    }
  }

  def getLocalIdentifier:Identifier = amberID

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
        consume(queuedInvocations.dequeue())
      }
    }
  }

  @inline
  private def registerSyncPromise(rootCtx:RootPromiseContext, ctx:PromiseContext): Unit ={
    syncPromiseRoot = rootCtx
    ongoingSyncPromises.add(ctx)
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

  @inline
  private def invokePromise(ctx:PromiseContext, call:ControlMessage[_]): Unit ={
    promiseContext = ctx
    promiseHandler(call)
  }


}

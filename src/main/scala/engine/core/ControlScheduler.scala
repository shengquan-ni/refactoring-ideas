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

  def schedule(cmd:ControlEvent): Unit = {
    cmd match{
      case ret:ReturnEvent =>
        if(afterComplete.contains(ret.context.id)){
          afterComplete(ret.context.id).invoke(this,ret.returnValue)
          afterComplete.remove(ret.context.id)
        }
        for(i <- afterGroupComplete){
          if(i.takeReturnValue(ret)){
            i.invoke(this)
            afterGroupComplete.remove(i)
          }
        }
      case remoteCall: PromiseEvent =>
        remoteCall.call.invoke()(remoteCall.context,this)
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

  def returning(value:Any)(implicit context:PromiseContext): Unit ={
    sendTo(context.sender, ReturnEvent(context,value))
  }


  def callAfterComplete[T](id:Long, cmd:T => Unit): Unit = {
    afterComplete(id) = AfterCompletion(cmd)
  }

  def callAfterGroupComplete[T](ids:Seq[Long], cmd: Seq[T] => Unit):Unit = {
    afterGroupComplete.add(AfterGroupCompletion(ids, cmd))
  }

  def getLocalIdentifier:AmberIdentifier = amberID

}

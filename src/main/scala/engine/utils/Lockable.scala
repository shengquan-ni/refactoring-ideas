package engine.utils

import engine.core.control.promise.PromiseContext

trait Lockable {

  private var context:PromiseContext = _

  def getOwner: PromiseContext = context

  def isLocked:Boolean = context != null

  def lock(context:PromiseContext): Unit ={
    this.context = context
  }

  def unlock():Unit = {
    this.context = null
  }

}

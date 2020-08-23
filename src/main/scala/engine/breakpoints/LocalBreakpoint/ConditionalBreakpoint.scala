package engine.breakpoints.LocalBreakpoint

import engine.common.Tuple


class ConditionalBreakpoint(val predicate:Tuple => Boolean)(implicit id:String, version:Long) extends LocalBreakpoint(id,version) {

  var _isTriggered = false

  override def accept(tuple: Tuple): Unit = {
     _isTriggered = predicate(tuple)
  }

  override def isTriggered: Boolean = _isTriggered

  override def isDirty: Boolean = isTriggered
}

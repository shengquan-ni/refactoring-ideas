package engine.breakpoints.LocalBreakpoint

import engine.common.Tuple


class ExceptionBreakpoint(triggeredTuple: Tuple, val error:Exception) extends LocalBreakpoint(null,0) {

  override def isTriggered: Boolean = triggeredTuple != null

  override def isDirty: Boolean = isTriggered

  override def accept(tuple: Tuple): Unit = {
    //empty
  }
}

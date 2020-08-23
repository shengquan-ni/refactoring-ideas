package engine.breakpoints.LocalBreakpoint

import engine.common.Tuple


abstract class LocalBreakpoint(val id:String,val version:Long) extends Serializable {

  def accept(tuple:Tuple)

  def isTriggered:Boolean

  def needUserFix:Boolean = isTriggered

  var isReported = false

  var triggeredTuple:Tuple = _

  def isDirty:Boolean
}

package engine.message

import engine.core.control.ControlInputChannel.InternalControlMessage

object ControlRecovery {
  case class RecoveryCompleted()
}

trait ControlRecovery {

  def triggerRecovery(): Unit

  def resetRecovery(): Unit

  def saveInputControlMessage(msg: InternalControlMessage)

  def saveOutputControlMessageID(id: Long)

  def ifMessageHasSent(id: Long): Boolean
}

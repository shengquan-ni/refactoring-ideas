package engine.core.controller

import engine.core.InternalActor
import engine.core.control.ControlInputChannel.InternalControlMessage
import engine.message.ControlRecovery


/** Since we assume controller will never fail, for now
 *  we don't implement recovery for controller.
 */
trait ControllerRecovery extends ControlRecovery {
  this:InternalActor =>

  override def triggerRecovery(): Unit = {

  }

  override def resetRecovery(): Unit = {

  }

  override def saveInputControlMessage(msg: InternalControlMessage): Unit = {

  }

  override def saveOutputControlMessageID(id: Long): Unit = {

  }

  override def ifMessageHasSent(id: Long): Boolean = {
    false
  }
}

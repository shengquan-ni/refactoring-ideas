package engine.core.control

import java.io.{ FileOutputStream, FileWriter, ObjectOutputStream }

import engine.common.OrderingEnforcer
import engine.common.identifier.Identifier
import engine.core.control.promise.{ PromiseEvent, PromiseManager }
import engine.core.InternalActor
import engine.core.control.ControlInputChannel.InternalControlMessage
import engine.core.control.ControlOutputChannel.ControlMessageAck
import engine.event.ControlEvent
import engine.message.{ InternalFIFOMessage, ControlRecovery }

import scala.collection.mutable

object ControlInputChannel {
  final case class InternalControlMessage(
    from: Identifier,
    sequenceNumber: Long,
    messageIdentifier: Long,
    command: ControlEvent,
  ) extends InternalFIFOMessage
}

trait ControlInputChannel {
  this: InternalActor with PromiseManager with ControlRecovery =>

  private val controlOrderingEnforcer =
    new mutable.AnyRefMap[Identifier, OrderingEnforcer[ControlEvent]]()

  def receiveControlMessage: Receive = {
    case msg @ InternalControlMessage(from, seq, messageID, cmd) =>
      sender ! ControlMessageAck(messageID)
      OrderingEnforcer.reorderMessage(controlOrderingEnforcer, from, seq, cmd) match {
        case Some(iterable) =>
          processControlEvents(iterable)
          saveInputControlMessage(msg)
        case None =>
          // discard duplicate
          println(s"receive duplicated: ${msg.command}")
      }
  }

  @inline
  def processControlEvents(iter: Iterable[ControlEvent]): Unit = {
    iter.foreach {
      case promise: PromiseEvent =>
        consume(promise)
      case other =>
      //skip
    }
  }

  @inline
  def processControlMessageForRecovery(msg: InternalControlMessage): Unit = {
    // specialized for recovery
    OrderingEnforcer.reorderMessage(
      controlOrderingEnforcer,
      msg.from,
      msg.sequenceNumber,
      msg.command,
    ) match {
      case Some(iterable) =>
        iterable.foreach {
          case promise: PromiseEvent =>
            consume(promise)
          case other =>
          //skip
        }
      case None =>
    }
  }

}

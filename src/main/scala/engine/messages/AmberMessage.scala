package engine.messages

import engine.common.AmberIdentifier


//base type of all messages
trait AmberMessage extends Serializable

//base type of all input messages
trait AmberFIFOMessage extends AmberMessage{
  val from:AmberIdentifier
  val sequenceNumber: Long
  val messageIdentifier: Long
}

//base type of all output messages
trait AmberOutputMessage extends AmberMessage

trait StopSignal



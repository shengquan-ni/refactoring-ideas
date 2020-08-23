package engine.core.messages

import akka.actor.typed.ActorRef

//base type of all messages
trait AmberMessage

//base type of all input messages
trait AmberInputMessage extends AmberMessage{
  val sender: ActorRef[AmberOutputMessage]
  val sequenceNumber: Long
  val messageIdentifier: Long
}

//base type of all output messages
trait AmberOutputMessage extends AmberMessage

trait StopSignal



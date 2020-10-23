package engine.message

import engine.common.identifier.Identifier

trait InternalFIFOMessage extends Serializable{
  val from:Identifier
  val sequenceNumber: Long
  val messageIdentifier: Long
}

package engine.message

import engine.common.identifier.AmberIdentifier

trait AmberFIFOMessage extends Serializable{
  val from:AmberIdentifier
  val sequenceNumber: Long
  val messageIdentifier: Long
}

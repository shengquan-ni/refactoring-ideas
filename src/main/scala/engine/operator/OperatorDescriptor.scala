package engine.operator


import java.util.UUID

import engine.common.identifier.Identifier

abstract class OperatorDescriptor extends Serializable {

  var operatorID: String = UUID.randomUUID.toString

  def operatorIdentifier: String = operatorID

  def operatorExecutor: OpExecConfig

  def getOutputSchema(schemas: Array[Schema]): Schema

  def validate(): Array[ConstraintViolation] = {
    Array()
  }

  override def hashCode: Int = HashCodeBuilder.reflectionHashCode(this)

  override def equals(that: Any): Boolean = EqualsBuilder.reflectionEquals(this, that)

  override def toString: String = ToStringBuilder.reflectionToString(this)

}

package engine.operators.simple

import engine.common.{LayerIdentifier, Tuple}
import engine.operators.PhysicalOperator

class MinusOne(val fieldIndex:Int) extends PhysicalOperator {
  var _tuple:Tuple = _
  var nextFlag = false

  override def accept(tuple: Tuple): Unit = {
    val rawData = tuple.toArray()
    assert(rawData(fieldIndex).isInstanceOf[Int])
    rawData(fieldIndex) = rawData(fieldIndex).asInstanceOf[Int] - 1
    _tuple = Tuple.fromIterable(rawData)
    nextFlag = true
  }

  override def noMore(): Unit = {

  }

  override def hasNext: Boolean = nextFlag

  override def next(): Tuple = {
    nextFlag = false
    _tuple
  }

  override def dispose(): Unit = {

  }

  override def initialize(): Unit = {

  }

  override def onUpstreamChanged(from: LayerIdentifier): Unit = {

  }

  override def onUpstreamExhausted(from: LayerIdentifier): Unit = {

  }

  override def discard(tuple: Tuple): Unit = {

  }
}

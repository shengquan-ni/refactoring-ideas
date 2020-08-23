package engine.operators.simple

import engine.common.{LayerIdentifier, Tuple}
import engine.operators.PhysicalOperator

class Sink extends PhysicalOperator {
  override def accept(tuple: Tuple): Unit = {
    println(tuple)
  }

  override def onUpstreamChanged(from: LayerIdentifier): Unit = {

  }

  override def onUpstreamExhausted(from: LayerIdentifier): Unit = {

  }

  override def noMore(): Unit = {

  }

  override def initialize(): Unit = {

  }

  override def hasNext: Boolean = false

  override def next(): Tuple = null

  override def dispose(): Unit = {

  }

  override def discard(tuple: Tuple): Unit = {

  }
}

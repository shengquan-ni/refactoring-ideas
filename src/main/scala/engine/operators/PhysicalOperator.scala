package engine.operators

import engine.common.{LayerIdentifier, Tuple}

trait PhysicalOperator {

  def isSource:Boolean = false

  @throws(classOf[Exception])
  def accept(tuple:Tuple): Unit

  def onUpstreamChanged(from:LayerIdentifier):Unit

  def onUpstreamExhausted(from:LayerIdentifier):Unit

  def noMore():Unit

  @throws(classOf[Exception])
  def initialize():Unit

  @throws(classOf[Exception])
  def hasNext:Boolean

  @throws(classOf[Exception])
  def next():Tuple

  @throws(classOf[Exception])
  def dispose():Unit

  @throws(classOf[Exception])
  def discard(tuple:Tuple): Unit
}

package engine.operators

import engine.common.{LayerIdentifier, Tuple}

trait SourcePhysicalOperator extends PhysicalOperator {
  override def isSource:Boolean = true

  @throws(classOf[Exception])
  def accept(tuple:Tuple): Unit ={
    //no impl
  }

  def onUpstreamChanged(from:LayerIdentifier):Unit ={
    //no impl
  }

  def onUpstreamExhausted(from:LayerIdentifier):Unit ={
    //no impl
  }

  def noMore():Unit ={
    //no impl
  }

  def discard(tuple: Tuple): Unit = {
    //no impl
  }

}

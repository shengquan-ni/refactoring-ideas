package engine.operators.simple

import engine.common.Tuple
import engine.operators.SourcePhysicalOperator


class Source(val limit:Int, val delay:Int = 0) extends SourcePhysicalOperator {

  var current = 0
  override def hasNext: Boolean = current < limit

  override def next(): Tuple = {
    current += 1
    if(delay > 0){
      Thread.sleep(delay)
    }
    Tuple(current)
  }

  override def dispose(): Unit = {

  }

  override def initialize(): Unit = {

  }
}

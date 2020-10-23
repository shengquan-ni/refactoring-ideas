package engine.common

case class EndWatermark() extends ITuple {
  override def length: Int = 0

  override def get(i: Int): Any = null

  override def toArray(): Array[Any] = null
}

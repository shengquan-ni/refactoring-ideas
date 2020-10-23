package engine.common

class InternalTuple(val data:Array[Any])extends ITuple {

  override def length: Int = data.length

  override def get(i: Int): Any = data(i)

  override def toArray(): Array[Any] = data
}

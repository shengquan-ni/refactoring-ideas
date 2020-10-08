package engine.common

class AmberTuple(val data:Array[Any])extends Tuple {

  override def length: Int = data.length

  override def get(i: Int): Any = data(i)

  override def toArray(): Array[Any] = data
}

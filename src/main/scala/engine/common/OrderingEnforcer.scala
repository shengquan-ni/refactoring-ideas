package engine.common

import scala.collection.mutable
import scala.reflect.ClassTag

class OrderingEnforcer[T:ClassTag] {

  var current = 0L
  val ofoMap = new mutable.LongMap[T]

  def ifDuplicated(sequenceNumber:Long):Boolean = sequenceNumber < current

  def pushAndRelease(sequenceNumber:Long, data:T): Array[T] ={
    if(sequenceNumber == current){
      val res = mutable.ArrayBuffer[T](data)
      current += 1
      while(ofoMap.contains(current)){
        res.addOne(ofoMap(current))
        ofoMap.remove(current)
        current += 1
      }
      res.toArray
    }else{
      ofoMap(sequenceNumber) = data
      Array.empty[T]
    }
  }
}

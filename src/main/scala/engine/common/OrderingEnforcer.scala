package engine.common

import scala.collection.mutable
import scala.reflect.ClassTag

object OrderingEnforcer{
  def reorderMessage[V: ClassTag](seqMap:mutable.AnyRefMap[AmberIdentifier,OrderingEnforcer[V]], sender: AmberIdentifier, seq:Long, command: V): Iterable[V] ={
    val entry = seqMap.getOrElseUpdate(sender,new OrderingEnforcer[V]())
    if(entry.ifDuplicated(seq)){
      Iterable.empty
    }else{
      entry.enforceFIFO(seq,command)
    }
  }
}



class OrderingEnforcer[T:ClassTag] {

  var current = 0L
  val ofoMap = new mutable.LongMap[T]

  def ifDuplicated(sequenceNumber:Long):Boolean = sequenceNumber < current

  def enforceFIFO(sequenceNumber:Long, data:T): Array[T] ={
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

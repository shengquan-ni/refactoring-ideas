package engine.core.control.promise

import scala.collection.mutable

case class AfterGroupCompletion[T](context: PromiseContext, id:Seq[Long], f: Seq[T] => Unit){

  val returnValues:mutable.ArrayBuffer[T] = mutable.ArrayBuffer[T]()
  val expectedIds:mutable.HashSet[Long] = mutable.HashSet[Long](id:_*)

  def takeReturnValue(returnEvent: ReturnEvent): Boolean ={
    if(expectedIds.contains(returnEvent.context.id)){
      expectedIds.remove(returnEvent.context.id)
      returnValues.append(returnEvent.returnValue.asInstanceOf[T])
    }
    expectedIds.isEmpty
  }

  def invoke(): Unit ={
    f(returnValues.toSeq)
  }
}

package engine.messages

import engine.core.{ControlScheduler, CoreProcessingUnit}
import engine.messages.RecursionHandler.Recursion

object RecursionHandler{
  case class Recursion(i:Int) extends AmberPromise
}

trait RecursionHandler extends WorkerControlHandler {
  this: CoreProcessingUnit with ControlScheduler =>

  registerHandler{
    case Recursion(i) =>
      if(i < 5){
        println(i)
        localPromise(Recursion(i+1)).onComplete{
          res:String =>
            println(res)
            returning(i.toString)
        }
      }else{
        returning(i.toString)
      }
  }

}

package engine.core.control.promise.utils

import engine.core.control.promise.utils.RecursionHandler.Recursion
import engine.core.control.promise.{AmberPromise, PromiseHandler, PromiseManager}

object RecursionHandler{
  case class Recursion(i:Int) extends AmberPromise[String]
}

trait RecursionHandler extends PromiseHandler {
  this: PromiseManager =>

  registerHandler{
    case Recursion(i) =>
      if(i < 5){
        println(i)
        after(schedule(Recursion(i+1))){
          res:String =>
            println(res)
            returning(i.toString)
        }
      }else{
        returning(i.toString)
      }
  }

}

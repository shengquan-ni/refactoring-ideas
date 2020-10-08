package engine.messages
import engine.common.AmberIdentifier
import engine.core.ControlScheduler

class RecursivePromise(depth:Int) extends AmberPromise{
  override def invoke()(implicit context:PromiseContext, scheduler:ControlScheduler): Unit = {
    println(s"executing recursive call with depth = $depth")
    if(depth == 5){
      returning("finished")
    }else{
      localPromise(new RecursivePromise(depth+1)).onComplete{
        str:String =>
          returning(str)
      }
    }
  }
}

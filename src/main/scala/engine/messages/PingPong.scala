package engine.messages

import engine.common.{AmberIdentifier, WorkflowIdentifier}
import engine.core.ControlScheduler

class Ping(id:Int,from:AmberIdentifier, to:AmberIdentifier) extends AmberPromise{
  override def invoke()(implicit context:PromiseContext, scheduler:ControlScheduler): Unit = {
    println(s"$id ping")
    if(id < 5){
      remotePromise(to,new Pong(id,to,from)).onComplete{
        ret:Int =>
          println(s"$id ping replied with value $ret!")
          returning(ret)
      }
    }else{
      returning(id)
    }
  }
}


class Pong(id:Int,from:AmberIdentifier, to:AmberIdentifier) extends AmberPromise{
  override def invoke()(implicit context:PromiseContext, scheduler:ControlScheduler): Unit = {
    println(s"$id pong")
    if(id < 5){
      remotePromise(to,new Ping(id+1,from,to)).onComplete{
        ret:Int =>
          println(s"$id pong replied with value $ret!")
          returning(ret)
      }
    }else{
      returning(id)
    }
  }
}


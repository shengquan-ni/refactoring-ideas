package engine.messages

import engine.common.AmberIdentifier
import engine.core.{ControlScheduler, CoreProcessingUnit}
import engine.messages.PingPongHandler.{Ping, Pong}

object PingPongHandler{
  case class Ping(i:Int, to:AmberIdentifier) extends AmberPromise

  case class Pong(i:Int, to:AmberIdentifier) extends AmberPromise
}


trait PingPongHandler extends WorkerControlHandler {
  this: CoreProcessingUnit with ControlScheduler =>

  registerHandler{
    case Ping(i,to) =>
      println(s"$i ping")
      if(i < 5){
        remotePromise(to, Pong(i+1,getLocalIdentifier)).onComplete{
          ret:Int =>
            println(s"$i ping replied with value $ret!")
            returning(ret)
        }
      }else{
        returning(i)
      }
    case Pong(i,to) =>
      println(s"$i pong")
      if(i < 5){
        remotePromise(to, Ping(i+1,getLocalIdentifier)).onComplete{
          ret:Int =>
            println(s"$i pong replied with value $ret!")
            returning(ret)
        }
      }else{
        returning(i)
      }
  }

}

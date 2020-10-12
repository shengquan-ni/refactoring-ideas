package engine.core.control.promise.utils

import engine.common.identifier.AmberIdentifier
import engine.core.control.promise.utils.PingPongHandler.{Ping, Pong}
import engine.core.control.promise.{AmberPromise, PromiseHandler, PromiseManager}

object PingPongHandler{
  case class Ping(i:Int, to:AmberIdentifier) extends AmberPromise[Int]

  case class Pong(i:Int, to:AmberIdentifier) extends AmberPromise[Int]
}


trait PingPongHandler extends PromiseHandler {
  this: PromiseManager =>

  registerHandler{
    case Ping(i,to) =>
      println(s"$i ping")
      if(i < 5){
        after(schedule(Pong(i+1,getLocalIdentifier), to)){
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
        after(schedule(Ping(i+1,getLocalIdentifier), to)){
          ret:Int =>
            println(s"$i pong replied with value $ret!")
            returning(ret)
        }
      }else{
        returning(i)
      }
  }

}

package engine.core.control.promise.utils

import engine.common.identifier.AmberIdentifier
import engine.core.control.promise.utils.PingPongHandler.{Ping, Pong}
import engine.core.control.promise.{InternalPromise, PromiseHandler, PromiseManager, SynchronizedInvocation}

object PingPongHandler{
  case class Ping(i:Int, end:Int, to:AmberIdentifier) extends InternalPromise[Int]

  case class Pong(i:Int, end:Int, to:AmberIdentifier) extends InternalPromise[Int] with SynchronizedInvocation
}


trait PingPongHandler extends PromiseHandler {
  this: PromiseManager =>

  registerHandler{
    case Ping(i,e,to) =>
      println(s"$i ping")
      if(i < e){
        after(schedule(Pong(i+1,e,getLocalIdentifier), to)){
          ret:Int =>
            println(s"$i ping replied with value $ret!")
            returning(ret)
        }
      }else{
        returning(i)
      }
    case Pong(i,e,to) =>
      println(s"$i pong")
      if(i < e){
        after(schedule(Ping(i+1,e,getLocalIdentifier), to)){
          ret:Int =>
            println(s"$i pong replied with value $ret!")
            returning(ret)
        }
      }else{
        returning(i)
      }
  }

}

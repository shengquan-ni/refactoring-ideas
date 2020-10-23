package engine.core.control.promise.utils

import java.util.concurrent.locks.ReentrantLock

object DummyState{
  final val Ready = 0
  final val NotReady = 1
}


trait DummyState {

  var currentState: Int = DummyState.Ready

  def checkState(state:Int): Unit ={
    if(state != currentState){
      throw new RuntimeException(s"current state is not $state")
    }
  }

}

package engine.core.control.promise.utils

import java.util.concurrent.locks.ReentrantLock

trait DummyStateComponent {

  var state:DummyState = new DummyState()

  class DummyState{
    var lockedVariable = 0
  }

}

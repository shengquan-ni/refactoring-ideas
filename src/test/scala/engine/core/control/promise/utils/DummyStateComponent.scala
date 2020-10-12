package engine.core.control.promise.utils

import java.util.concurrent.locks.ReentrantLock

import engine.utils.Lockable

trait DummyStateComponent {

  var state:DummyState = new DummyState()

  class DummyState extends Lockable{
    var lockedVariable = 0
  }

}

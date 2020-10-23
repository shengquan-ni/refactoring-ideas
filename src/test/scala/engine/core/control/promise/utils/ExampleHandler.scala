package engine.core.control.promise.utils

import engine.common.identifier.{ActorIdentifier, Identifier}
import engine.core.control.ControlMessage
import engine.core.control.promise.utils.ExampleHandler.{Ask, Init}
import engine.core.control.promise.{InternalPromise, PromiseHandler, PromiseManager, PromiseCompleted}

object ExampleHandler{
  case class Init() extends ControlMessage[PromiseCompleted]
  case class Ask() extends ControlMessage[String]
}


trait ExampleHandler  extends PromiseHandler {
  this: PromiseManager with DummyState =>

  registerHandler{
    case Init() =>
      checkState(DummyState.Ready)
      val promise = schedule(Ask(),ActorIdentifier(1))
      promise.onSuccess{
        ret =>
          println(s"$getLocalIdentifier received: $ret")
          returning()
      }
      promise.onFailure{
        failure =>
          println(failure)
          returning()
      }
    case Ask() =>
      checkState(DummyState.Ready)
      returning(s"reply from actor $getLocalIdentifier")
  }

}

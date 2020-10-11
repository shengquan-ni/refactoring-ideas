package engine.core.control.promise

case class AfterCompletion[T](context: PromiseContext, f: T => Unit){
  def invoke(returnValue: Any): Unit = {
    f(returnValue.asInstanceOf[T])
  }
}

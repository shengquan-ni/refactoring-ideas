package engine.core.control.promise
import com.twitter.util.Promise

object InternalPromise{
  def apply[T](ctx:PromiseContext): InternalPromise[T] = new InternalPromise[T](ctx)
}

class InternalPromise[T](val ctx:PromiseContext) extends Promise[T]{

  type returnType = T

  override def setValue(result: returnType): Unit = super.setValue(result)
}

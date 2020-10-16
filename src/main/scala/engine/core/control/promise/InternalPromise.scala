package engine.core.control.promise
import scala.reflect.runtime.universe._


trait InternalPromise[T]{

}

trait VoidInternalPromise extends InternalPromise[Nothing]
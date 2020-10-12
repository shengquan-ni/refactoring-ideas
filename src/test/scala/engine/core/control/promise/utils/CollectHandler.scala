package engine.core.control.promise.utils

import engine.common.identifier.AmberIdentifier
import engine.core.control.promise.utils.CollectHandler.Collect
import engine.core.control.promise.utils.NestedHandler.Pass
import engine.core.control.promise.{AmberPromise, PromiseHandler, PromiseManager}
object CollectHandler{
  case class Collect(workers:Seq[AmberIdentifier]) extends AmberPromise[String]
}

trait CollectHandler extends PromiseHandler {
  this: PromiseManager =>

  registerHandler {
    case Collect(workers) =>
      val phrase = "encounters a problem"
      val parts = phrase.split(" ")
      println(s"one programmer $phrase, he decides to use multi-thread to solve it.")
      val tasks = parts.indices.map(i => schedule(Pass(parts(i)), workers(i%workers.size)))
      after(tasks:_*){
        res =>
          println(s"now he ${res.mkString(" ")}")
          returning("finished")
      }
  }
}

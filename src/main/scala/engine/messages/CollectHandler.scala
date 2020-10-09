package engine.messages

import engine.common.AmberIdentifier
import engine.core.{ControlScheduler, CoreProcessingUnit}
import engine.messages.CollectHandler.Collect
import engine.messages.NestedHandler.Pass

import scala.collection.mutable
object CollectHandler{
  case class Collect(workers:Seq[AmberIdentifier]) extends AmberPromise
}

trait CollectHandler extends WorkerControlHandler {
  this:CoreProcessingUnit with ControlScheduler =>

  registerHandler {
    case Collect(workers) =>
      val phrase = "encounters a problem"
      val parts = phrase.split(" ")
      println(s"one programmer $phrase, he decides to use multi-thread to solve it.")
      val tasks = parts.indices.map(i => remotePromise(workers(i%workers.size), Pass(parts(i))))
      GroupedFutures(tasks:_*)(this).onComplete{
        res:Seq[String] =>
          println(s"now he ${res.mkString(" ")}")
          returning("finished")
      }
  }
}

package engine.core.control.promise.utils

import engine.common.identifier.Identifier
import engine.core.control.promise.utils.CollectHandler.{ Collect, GenerateNumber }
import engine.core.control.promise.utils.NestedHandler.Pass
import engine.core.control.promise.{ InternalPromise, PromiseBody, PromiseHandler, PromiseManager }

import scala.util.Random
object CollectHandler {
  case class Collect(workers: Seq[Identifier]) extends PromiseBody[String]
  case class GenerateNumber() extends PromiseBody[Int]
}

trait CollectHandler extends PromiseHandler {
  this: PromiseManager =>

  registerHandler {
    case Collect(workers) =>
      println(s"start collecting numbers.")
      val tasks = workers.indices.map(i => (GenerateNumber(), workers(i)))
      val p = schedule(tasks: _*)
      p.map { res =>
        println(s"collected: ${res.mkString(" ")}")
        returning("finished")
      }
    case GenerateNumber() =>
      returning(Random.nextInt())
  }
}

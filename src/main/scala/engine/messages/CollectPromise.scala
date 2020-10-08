package engine.messages
import engine.common.AmberIdentifier
import engine.core.ControlScheduler

class CollectPromise(workers:Seq[AmberIdentifier]) extends AmberPromise {
  override def invoke()(implicit context:PromiseContext, scheduler:ControlScheduler): Unit = {
    val phrase = "encounters a problem"
    println(s"one programmer $phrase, he decides to use multi-thread to solve it.")
    val tasks = phrase.indices.map(i => remotePromise(workers(i%workers.size), Pass(phrase(i))))
    GroupedFutures(tasks:_*).onComplete{
      res:Seq[Char] =>
        println(s"now he ${res.mkString("")}")
        returning("finished")
    }
  }
}


case class Pass[T](s: T) extends AmberPromise{
  override def invoke()(implicit context: PromiseContext, scheduler: ControlScheduler): Unit = {
    returning(s)
  }
}

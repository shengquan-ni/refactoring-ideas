package engine.core.control.promise

import engine.common.identifier.AmberIdentifier

object PromiseContext{
  def apply(sender:AmberIdentifier, id:Long): RootPromiseContext = RootPromiseContext(sender, id)
  def apply(sender:AmberIdentifier, id:Long, root: RootPromiseContext): ChildPromiseContext = ChildPromiseContext(sender, id, root)
}


sealed trait PromiseContext{
  def sender:AmberIdentifier
  def id:Long
}


case class RootPromiseContext(sender:AmberIdentifier, id:Long) extends PromiseContext


case class ChildPromiseContext(sender:AmberIdentifier, id:Long, root:RootPromiseContext) extends PromiseContext




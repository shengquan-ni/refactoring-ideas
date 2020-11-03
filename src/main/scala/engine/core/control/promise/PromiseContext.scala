package engine.core.control.promise

import engine.common.identifier.Identifier

object PromiseContext {
  def apply(sender: Identifier, id: Long): RootPromiseContext = RootPromiseContext(sender, id)
  def apply(sender: Identifier, id: Long, root: RootPromiseContext): ChildPromiseContext =
    ChildPromiseContext(sender, id, root)
}

sealed trait PromiseContext {
  def sender: Identifier
  def id: Long
}

case class RootPromiseContext(sender: Identifier, id: Long) extends PromiseContext

case class ChildPromiseContext(sender: Identifier, id: Long, root: RootPromiseContext)
  extends PromiseContext

package engine.common.identifier

trait Identifier

object Identifier {

  case class EmptyIdentifier() extends Identifier

  lazy val Controller: ControllerIdentifier = ControllerIdentifier()
  lazy val Client: ClientIdentifier = ClientIdentifier()
  lazy val None: EmptyIdentifier = EmptyIdentifier()

}

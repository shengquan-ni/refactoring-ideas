package engine.common.identifier

trait AmberIdentifier

object AmberIdentifier{

  case class EmptyIdentifier() extends AmberIdentifier

  lazy val Controller: ControllerIdentifier = ControllerIdentifier()
  lazy val Client:ClientIdentifier = ClientIdentifier()
  lazy val None:EmptyIdentifier = EmptyIdentifier()

}

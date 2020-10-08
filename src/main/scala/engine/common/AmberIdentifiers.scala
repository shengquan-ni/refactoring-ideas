package engine.common

trait AmberIdentifier

trait AmberRemoteIdentifier extends AmberIdentifier

object AmberIdentifier{
  lazy val Controller: ControllerIdentifier = ControllerIdentifier()
  lazy val Client:ClientIdentifier = ClientIdentifier()
  lazy val None:EmptyIdentifier = EmptyIdentifier()

}

//Workflow related information(Operator, Index)
case class WorkerIdentifier(physicalOperatorIdentifier: PhysicalOperatorIdentifier, workerID:Long) extends AmberRemoteIdentifier{

  override def toString: String = s"WorkerID($simpleString)"

  lazy val simpleString:String = {
    val workflowID = physicalOperatorIdentifier.operatorIdentifier.workflowIdentifier.workflowID
    val operatorID = physicalOperatorIdentifier.operatorIdentifier.operatorID
    val layerID = physicalOperatorIdentifier.layerID
    s"$workflowID-$operatorID-$layerID-$workerID"
  }
}

case class LinkIdentifier(from: PhysicalOperatorIdentifier, to:PhysicalOperatorIdentifier) extends AmberIdentifier

case class PhysicalOperatorIdentifier(operatorIdentifier: OperatorIdentifier, layerID: Long) extends AmberIdentifier{
  override def toString: String = s"PhysicalOperatorID($simpleString)"

  lazy val simpleString:String = {
    val workflowID = operatorIdentifier.workflowIdentifier.workflowID
    val operatorID = operatorIdentifier.operatorID
    s"$workflowID-$operatorID-$layerID"
  }
}

case class OperatorIdentifier(workflowIdentifier: WorkflowIdentifier, operatorID:Long) extends AmberIdentifier{
  override def toString: String = s"OperatorID($simpleString)"

  lazy val simpleString:String = {
    val workflowID = workflowIdentifier.workflowID
    s"$workflowID-$operatorID"
  }
}

case class WorkflowIdentifier(workflowID:Long) extends AmberIdentifier

case class ControllerIdentifier() extends AmberRemoteIdentifier

case class ClientIdentifier() extends AmberRemoteIdentifier

case class EmptyIdentifier() extends AmberIdentifier

case class ActorIdentifier(id:Long) extends AmberIdentifier

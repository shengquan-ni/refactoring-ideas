package engine.common

trait AmberIdentifier

//Workflow related information(Operator, Edge, Index)
class WorkerIdentifier(workflowID:Long, operatorID:Long, layerID:Long, val workerID:Long) extends LayerIdentifier(workflowID, operatorID, layerID) {
  lazy val workerIDString: String = s"$workflowID/$operatorID/$layerID/$workerID"
}

class LayerIdentifier(workflowID:Long, operatorID:Long, val layerID:Long) extends OperatorIdentifier(workflowID, operatorID) {
  lazy val layerIDString: String = s"$workflowID/$operatorID/$layerID"
  def toLayerIdentifier = new LayerIdentifier(workflowID,operatorID,layerID)
}

class OperatorIdentifier(workflowID:Long, val operatorID:Long) extends WorkflowIdentifier(workflowID) {
  def toOperatorIdentifier = new OperatorIdentifier(workflowID,operatorID)
  lazy val operatorIDString: String = s"$workflowID/$operatorID"
}

class WorkflowIdentifier(val workflowID:Long) extends AmberIdentifier{
  lazy val workflowIDString: String = workflowID.toString
  def toWorkflowIdentifier = new WorkflowIdentifier(workflowID)
}
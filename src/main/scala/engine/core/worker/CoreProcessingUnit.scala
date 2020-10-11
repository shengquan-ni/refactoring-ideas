package engine.core.worker

import java.util.concurrent.Executors

import engine.common.Tuple
import engine.core.data.DataTransferPolicy
import engine.core.worker.utils.PauseSupport
import engine.event.DataEvent

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, ExecutionContextExecutor}

trait CoreProcessingUnit extends PauseSupport {

  // DP Thread
  val dataProcessExecutor: ExecutionContextExecutor = ExecutionContext.fromExecutor(Executors.newSingleThreadExecutor)
  // Event Queue
  val processingQueue = new mutable.ArrayDeque[DataEvent]
  var currentConsumingTuple: Tuple = _
  var processedCount: Long = 0L
  var generatedCount: Long = 0L
  var generatedAfterAccept: Long = 0L
  val outputPolicies =  new Array[DataTransferPolicy](0)

  def process(evt:DataEvent): Unit ={

  }

}

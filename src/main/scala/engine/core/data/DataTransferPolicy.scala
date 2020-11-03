package engine.core.data

import akka.actor.{ Actor, ActorRef }
import engine.common.ITuple
import engine.common.identifier.Identifier
import engine.event.DataEvent

trait DataTransferPolicy {

  def consumeTuples(tuples: IterableOnce[ITuple]): Array[(Identifier, DataEvent)]

}

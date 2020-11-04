package clustering

import akka.actor.{ Actor, ActorLogging }
import clustering.ClusterListener.GetAvailableNodeAddresses
import engine.common.Constants

class SingleNodeListener extends Actor with ActorLogging {
  Constants.defaultNumWorkers = 2
  override def receive: Receive = { case GetAvailableNodeAddresses =>
    sender ! Array(context.self.path.address)
  }
}

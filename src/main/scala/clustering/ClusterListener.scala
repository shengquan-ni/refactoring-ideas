package clustering

import akka.actor.{ Actor, ActorLogging, Address, ExtendedActorSystem }
import akka.cluster.{ Cluster, Member }
import akka.cluster.ClusterEvent.{
  InitialStateAsEvents,
  MemberEvent,
  MemberRemoved,
  MemberUp,
  UnreachableMember,
}
import engine.common.Constants

import scala.collection.mutable

object ClusterListener {
  final case class GetAvailableNodeAddresses()
}

class ClusterListener extends Actor with ActorLogging {

  val cluster = Cluster(context.system)
  val availableNodeAddresses = new mutable.HashSet[Address]()

  // subscribe to cluster changes, re-subscribe when restart
  override def preStart(): Unit = {
    cluster.subscribe(
      self,
      initialStateMode = InitialStateAsEvents,
      classOf[MemberEvent],
      classOf[UnreachableMember],
    )
  }
  override def postStop(): Unit = cluster.unsubscribe(self)

  def onMemberRemoved(member: Member): Unit = {
    if (
      context.system.asInstanceOf[ExtendedActorSystem].provider.getDefaultAddress == member.address
    ) {
      if (Constants.masterNodeAddr != null) {
        availableNodeAddresses.remove(self.path.address)
        Constants.dataset -= Constants.dataVolumePerNode
        Constants.defaultNumWorkers -= Constants.numWorkerPerNode
      }
    } else {
      if (Constants.masterNodeAddr != member.address.host.get) {
        availableNodeAddresses.remove(member.address)
        Constants.dataset -= Constants.dataVolumePerNode
        Constants.defaultNumWorkers -= Constants.numWorkerPerNode
      }
    }
  }

  def receive: Receive = {
    case MemberUp(member) =>
      if (
        context.system
          .asInstanceOf[ExtendedActorSystem]
          .provider
          .getDefaultAddress == member.address
      ) {
        if (Constants.masterNodeAddr != null) {
          availableNodeAddresses.add(self.path.address)
          Constants.dataset += Constants.dataVolumePerNode
          Constants.defaultNumWorkers += Constants.numWorkerPerNode
        }
      } else {
        if (Constants.masterNodeAddr != member.address.host.get) {
          availableNodeAddresses.add(member.address)
          Constants.dataset += Constants.dataVolumePerNode
          Constants.defaultNumWorkers += Constants.numWorkerPerNode
        }
      }
      println(
        "---------Now we have " + availableNodeAddresses.size + " nodes in the cluster---------"
      )
      println("dataset: " + Constants.dataset + " numWorkers: " + Constants.defaultNumWorkers)
    case UnreachableMember(member) =>
      onMemberRemoved(member)
      log.info("Member detected as unreachable: {}", member)
    case MemberRemoved(member, previousStatus) =>
      onMemberRemoved(member)
      log.info("Member is Removed: {} after {}", member.address, previousStatus)
    case _: MemberEvent                            => // ignore
    case ClusterListener.GetAvailableNodeAddresses => sender ! availableNodeAddresses.toArray
  }

}

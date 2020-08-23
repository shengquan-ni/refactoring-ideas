package engine.core.worker.components

import akka.actor.typed.ActorRef
import engine.common.{AmberIdentifier, Tuple, WorkerIdentifier}
import engine.core.data.send.ActorSender.MessageInTransit
import engine.core.data.send.SendLayerComponent
import engine.core.data.send.policy.DataTransferPolicy
import engine.core.messages.data.{EndSending, StartSending}
import engine.core.messages.{AmberEvent, AmberInputMessage, AmberOutputMessage}
import engine.core.worker.DataEvent

trait WorkerSendLayerComponent extends SendLayerComponent {
  override val sendLayer: WorkerSendLayer

  class WorkerSendLayer(val myIdentifier: WorkerIdentifier,
                         val senderActor: ActorRef[AmberOutputMessage]) extends SendLayer {

    private var dataTransferPolicies = new Array[DataTransferPolicy](0)
    private var deactivated = false

    def deactivate(): Unit ={
      //no more link can be added
      clearAllDownStreams()
      deactivated = true
    }

    def addDataTransferPolicy(policy: DataTransferPolicy): Unit ={
      //update link if duplicated
      removeDataTransferPolicy(policy.receiverIdentifier)
      //initialize policy
      policy.initialize()
      policy.attachTo(senderActor)
      broadcastTo(policy,StartSending(myIdentifier))
      if(deactivated){
        broadcastTo(policy,EndSending())
      }else{
        dataTransferPolicies :+= policy
      }
    }

    def removeDataTransferPolicy(withReceiver:AmberIdentifier):Unit ={
      dataTransferPolicies.find(_.receiverIdentifier == withReceiver) match {
        case Some(policy) =>
          broadcastTo(policy,EndSending())
        case None =>
      }
      dataTransferPolicies = dataTransferPolicies.filter(_.receiverIdentifier != withReceiver)
    }

    def sendTo(receiver:ActorRef[AmberInputMessage], event: AmberEvent): Unit = {
      if(receiver != null){
        senderActor ! MessageInTransit(receiver,event)
      }
    }


    private def broadcastTo(policy:DataTransferPolicy, event:DataEvent): Unit ={
      policy.routees.foreach{
        routee=>
          senderActor ! MessageInTransit(routee,event)
      }
    }

    private def clearAllDownStreams(): Unit ={
      dataTransferPolicies.foreach(broadcastTo(_,EndSending()))
      dataTransferPolicies = new Array[DataTransferPolicy](0)
    }

    def addToDownstream(tuple:Tuple): Unit = {
      if(tuple == null) return
      //TODO: save tuple to an external storage to persist
      var i = 0
      while(i < dataTransferPolicies.length){
        dataTransferPolicies(i).accept(tuple)
        i += 1
      }
    }



  }

}

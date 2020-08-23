package engine.core.messages

import akka.actor.typed.ActorRef
import engine.common.WorkerIdentifier
import engine.core.messages.data.{EndSending, Payload, StartSending}
import engine.core.worker.{RoundTripControlEventForWorker, Worker}



// final case class SourceStartReply(workerIdentifier: WorkerIdentifier) extends ReplyControlLogic[Controller]


final case class SourceStartToWorker(id:Long, replyTo: ActorRef[AmberInputMessage]) extends RoundTripControlEventForWorker{

  override def onArrive(receiver: Worker): Unit = {
    if(receiver.processSupport.coreLogic.isSource){
      //set expected End flags
      val myId = receiver.sendLayer.myIdentifier
      receiver.processSupport.expectedEndFlags = Map(myId.layerIDString -> Set(myId))
      //activate self
      receiver.sendLayer.sendTo(receiver.self,StartSending(myId))
      receiver.sendLayer.sendTo(receiver.self,Payload(Array.empty))
      receiver.sendLayer.sendTo(receiver.self,EndSending())
    }
    //send reply
    // receiver.sendLayer.sendTo(replyTo, ControlEvent(id,SourceStartReply(receiver.sendLayer.myIdentifier)))
  }
}

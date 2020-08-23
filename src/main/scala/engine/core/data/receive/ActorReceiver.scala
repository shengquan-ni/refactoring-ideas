package engine.core.data.receive

import akka.actor.typed.{ActorRef, Behavior, PostStop, Signal}
import akka.actor.typed.scaladsl.{AbstractBehavior, ActorContext, Behaviors}
import engine.core.data.receive.ActorReceiver.AmberFIFOMessage
import engine.core.data.send.ActorSender.Ack
import engine.core.data.send.SendLayerComponent
import engine.core.messages.{AmberEvent, AmberInputMessage, AmberOutputMessage, ControlEvent, StopSignal}


object ActorReceiver{
  //Message received by receiver
  final case class AmberFIFOMessage[T<:AmberEvent](sender: ActorRef[AmberOutputMessage], sequenceNumber: Long, messageIdentifier: Long, command: T) extends AmberInputMessage
}


abstract class ActorReceiver(context: ActorContext[AmberInputMessage])
  extends AbstractBehavior[AmberInputMessage](context)
    with ReceiveLayerComponent
    with SendLayerComponent {

  override def onMessage(msg: AmberInputMessage): Behavior[AmberInputMessage] = {
    msg.sender ! Ack(msg.messageIdentifier)
    receiveLayer.onReceive(msg).foreach(eventHandler)
    msg match{
      case AmberFIFOMessage(sender, sequenceNumber, messageIdentifier, command) =>
        if(command.isInstanceOf[StopSignal])
          Behaviors.stopped
        else
          Behaviors.same
      case other =>
        Behaviors.same
    }
  }

  protected def discardOtherEvents:PartialFunction[AmberEvent,Unit] = {
    case event =>
      context.log.info(s"Discarding event =  $event")
  }

  protected lazy val eventHandler: PartialFunction[AmberEvent,Unit] = discardOtherEvents

  override def onSignal: PartialFunction[Signal, Behavior[AmberInputMessage]] = {
    case PostStop =>
      onStop()
      Behaviors.stopped
  }

  protected def onStop(): Unit ={
    //clean up
  }
}

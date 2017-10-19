package fr.univ_lille.cristal.emeraude.chasqui.core.synchronization

import akka.actor.ActorRef
import fr.univ_lille.cristal.emeraude.chasqui.core.Node.ScheduleMessage
import fr.univ_lille.cristal.emeraude.chasqui.core._

import scala.collection.mutable

/**
  * Created by guille on 19/04/17.
  */
class ManualSynchronizerStrategy extends SynchronizerStrategy {
  override def registerNode(node: Node): Unit = {
    //Do nothing
    //We do not track any node
  }

  def notifyFinishedTime(nodeActor: ActorRef, node: Node, t: Long, messageDelta: Int): Unit = {
    //Do nothing
    //I leave the user with the entire responsibility of synchronizing
  }

  override def handleSynchronizationMessage(message: SynchronizationMessage, sender: ActorRef, receiver: Node, t: Long): Unit = {
    //Nothing
  }

  override def sendMessage(senderNode: NodeImpl, receiverActor: ActorRef, messageTimestamp: Long, message: Any): Unit = {
    receiverActor ! ScheduleMessage(message, messageTimestamp, senderNode.getActorRef)
  }

  override def scheduleMessage(receiverNode: NodeImpl, senderActor: ActorRef, messageTimestamp: Long, message: Any): Unit = {
    if (messageTimestamp < receiverNode.getCurrentSimulationTime) {
      //The message is in the past.
      //This is a Causality error
      if (!message.isInstanceOf[SynchronizationMessage]){
        receiverNode.getCausalityErrorStrategy.handleCausalityError(messageTimestamp, receiverNode.getCurrentSimulationTime, receiverNode, senderActor, message)
      }
      return
    }
    receiverNode.handleIncomingMessage(message, senderActor)
    receiverNode.notifyFinishedQuantum()
  }

  override def getMessageQueue = new mutable.PriorityQueue[Message]()(Ordering.fromLessThan((s1, s2) => true))
}

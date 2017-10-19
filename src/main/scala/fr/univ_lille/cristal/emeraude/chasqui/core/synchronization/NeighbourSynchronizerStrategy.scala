package fr.univ_lille.cristal.emeraude.chasqui.core.synchronization

import akka.actor.ActorRef
import fr.univ_lille.cristal.emeraude.chasqui.core.Node.ScheduleMessage
import fr.univ_lille.cristal.emeraude.chasqui.core._

import scala.collection.mutable

/**
  * Created by guille on 29/05/17.
  */

case class FinishedQuantum(nextQuantum: Long) extends SynchronizationMessage

class NeighbourSynchronizerStrategy extends SynchronizerStrategy {

  val neighboursFinished: mutable.Set[ActorRef] = new mutable.HashSet[ActorRef]()

  override def registerNode(node: Node): Unit = {
    //Do nothing
    //We do not track any node
  }

  def notifyFinishedTime(nodeActor: ActorRef, node: Node, t: Long, queueSize: Int, messageDelta: Int): Unit = {
    node.broadcastMessageToIncoming(FinishedQuantum(t), t)
    this.checkAdvanceSimulation(node, t)
  }

  def handleSynchronizationMessage(message: SynchronizationMessage, sender: ActorRef, receiver: Node, t: Long): Unit = {
    neighboursFinished += sender
    this.checkAdvanceSimulation(receiver, t)
  }

  private def checkAdvanceSimulation(receiver: Node, t: Long): Unit = {
    val allNeighboursAreReady = receiver.getIngoingConnections.forall(node => neighboursFinished.contains(node))
    if (allNeighboursAreReady && !receiver.hasPendingMessagesOfTimestamp(t)) {
      neighboursFinished.clear()
      receiver.scheduleSimulationAdvance(t + 1)
    }
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

    if (receiverNode.getCurrentSimulationTime == messageTimestamp){
      receiverNode.handleIncomingMessage(message, senderActor)
    } else {
      queueMessage(receiverNode, senderActor, messageTimestamp, message)
    }
    receiverNode.notifyFinishedQuantum()
  }

  private def queueMessage(receiverNode: NodeImpl, senderActor: ActorRef, messageTimestamp: Long, message: Any) = {
    receiverNode.queueMessage(message, messageTimestamp, senderActor)
  }
}

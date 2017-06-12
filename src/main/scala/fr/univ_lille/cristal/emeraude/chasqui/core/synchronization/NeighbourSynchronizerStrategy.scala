package fr.univ_lille.cristal.emeraude.chasqui.core.synchronization

import akka.actor.ActorRef
import fr.univ_lille.cristal.emeraude.chasqui.core._

import scala.collection.mutable

/**
  * Created by guille on 29/05/17.
  */

case class FinishedQuantum(nextQuantum: Long) extends SynchronizationMessage

class NeighbourSynchronizerStrategy extends SynchronizerStrategy {

  val neighboursFinished: mutable.Set[Messaging] = new mutable.HashSet[Messaging]()

  override def registerNode(node: ActorRef): Unit = {
    //Do nothing
    //We do not track any node
  }

  def notifyFinishedTime(nodeActor: ActorRef, node: Node, t: Long, queueSize: Int, messageDelta: Int): Unit = {
    node.broadcastMessageToIncoming(FinishedQuantum(t), t)
    this.checkAdvanceSimulation(node, t)
  }

  def handleSynchronizationMessage(message: SynchronizationMessage, sender: Messaging, receiver: Node, t: Long): Unit = {
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
}

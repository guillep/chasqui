package fr.univ_lille.cristal.emeraude.chasqui.core

import scala.collection.mutable

/**
  * Created by guille on 29/05/17.
  */

case class FinishedQuantum(nextQuantum: Option[Long]) extends SynchronizationMessage

class NeighbourSynchronizerStrategy extends SynchronizerStrategy {

  var nextQuantum: Long = Long.MaxValue
  val neighboursFinished: mutable.Set[Messaging] = new mutable.HashSet[Messaging]()

  override def registerNode(node: Node): Unit = {
    //Do nothing
    //We do not track any node
  }

  def notifyFinishedTime(node: Node, t: Long, queueSize: Int, messageDelta: Int): Unit = {
    node.broadcastMessageToIncoming(FinishedQuantum(node.getRealIncomingQuantum()), t)
  }

  def handleSynchronizationMessage(message: SynchronizationMessage, sender: Messaging, receiver: Node) = {
    val finishedQuantumMessage = message.asInstanceOf[FinishedQuantum]

    if (finishedQuantumMessage.nextQuantum.isDefined){
      nextQuantum = Math.min(nextQuantum, finishedQuantumMessage.nextQuantum.get)
    }
    neighboursFinished += sender
    val allNeighboursAreReady = receiver.getIngoingConnections.forall(node => neighboursFinished.contains(node))

    if (allNeighboursAreReady && receiver.hasPendingMessages()) {
      receiver.advanceSimulationTime(nextQuantum)
    }
  }
}

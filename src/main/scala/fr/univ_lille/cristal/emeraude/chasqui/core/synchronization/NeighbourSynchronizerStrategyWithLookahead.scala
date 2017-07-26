package fr.univ_lille.cristal.emeraude.chasqui.core.synchronization

import akka.actor.ActorRef
import fr.univ_lille.cristal.emeraude.chasqui.core._

import scala.collection.mutable

/**
  * Created by guille on 29/05/17.
  */

case class FinishedQuantumWithLookahead(finishedQuantum: Long, nextQuantum: Option[Long]) extends SynchronizationMessage {
  def hasPendingMessages(): Boolean = nextQuantum.isDefined
  def getFinishedQuantum(): Long = finishedQuantum
}

class NeighbourSynchronizerStrategyWithLookahead extends SynchronizerStrategy {

  val ingoingNeighboursFinished: mutable.HashMap[ActorRef, FinishedQuantumWithLookahead] = new mutable.HashMap[ActorRef, FinishedQuantumWithLookahead]()

  override def registerNode(node: ActorRef): Unit = {
    //Do nothing
    //We do not track any node
  }

  def notifyFinishedTime(nodeActor: ActorRef, node: Node, t: Long, queueSize: Int, messageDelta: Int): Unit = {
    node.broadcastMessageToOutgoing(FinishedQuantumWithLookahead(t, node.getRealIncomingQuantum()), t)
    this.checkAdvanceSimulation(node, t)
  }

  def handleSynchronizationMessage(message: SynchronizationMessage, sender: ActorRef, receiver: Node, t: Long): Unit = {
    ingoingNeighboursFinished.update(sender, message.asInstanceOf[FinishedQuantumWithLookahead])
    this.checkAdvanceSimulation(receiver, t)
  }

  private def checkAdvanceSimulation(receiver: Node, t: Long): Unit = {

    // All neighbours are ready if:
    // - either they announced they finished and that they have no more pending messages (t=None)
    // - or they announced they finished a t >= current node simulation time
    val allNeighboursAreReady = receiver.getIngoingConnections.forall(node =>
      ingoingNeighboursFinished.contains(node) &&
        (!ingoingNeighboursFinished(node).hasPendingMessages()
          || ingoingNeighboursFinished(node).getFinishedQuantum() >= t))

    if (allNeighboursAreReady && !receiver.hasPendingMessagesOfTimestamp(t)) {
      val nextQuantum = if (ingoingNeighboursFinished.isEmpty) {
        None
      } else {
        ingoingNeighboursFinished.map(_._2.nextQuantum).reduce((maybeNextQuantum1, maybeNextQuantum2) => {
          //Find the minimum of two Option[Long]
          List(maybeNextQuantum1, maybeNextQuantum2).flatten match {
            case Nil => None
            case xs => Some(xs.min)
          } })
      }
      if (nextQuantum.isDefined){
        receiver.scheduleSimulationAdvance(nextQuantum.get)
      } else {
        println("nothing?")
      }
    }
  }
}

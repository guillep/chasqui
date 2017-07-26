package fr.univ_lille.cristal.emeraude.chasqui.core.synchronization

import akka.actor.ActorRef
import fr.univ_lille.cristal.emeraude.chasqui.core._

import scala.collection.mutable

/**
  * Created by guille on 29/05/17.
  */

case class FinishedQuantumWithLookahead(nextQuantum: Option[Long]) extends SynchronizationMessage

class NeighbourSynchronizerStrategyWithLookahead extends SynchronizerStrategy {

  val ingoingNeighboursFinished: mutable.Set[(ActorRef, Option[Long])] = new mutable.HashSet[(ActorRef, Option[Long])]()

  override def registerNode(node: ActorRef): Unit = {
    //Do nothing
    //We do not track any node
  }

  def notifyFinishedTime(nodeActor: ActorRef, node: Node, t: Long, queueSize: Int, messageDelta: Int): Unit = {
    println(s"$nodeActor finished at $t")
    node.broadcastMessageToOutgoing(FinishedQuantumWithLookahead(node.getRealIncomingQuantum()), t)
    this.checkAdvanceSimulation(node, t)
  }

  def handleSynchronizationMessage(message: SynchronizationMessage, sender: ActorRef, receiver: Node, t: Long): Unit = {
    val nextPossibleQuantum = message.asInstanceOf[FinishedQuantumWithLookahead].nextQuantum
    ingoingNeighboursFinished += (sender -> nextPossibleQuantum)
    println(s"$sender notified finished at $nextPossibleQuantum")
    this.checkAdvanceSimulation(receiver, t)
  }

  private def checkAdvanceSimulation(receiver: Node, t: Long): Unit = {
    val allNeighboursAreReady = receiver.getIngoingConnections.forall(node => ingoingNeighboursFinished.map(_._1).contains(node))
    if (allNeighboursAreReady && !receiver.hasPendingMessagesOfTimestamp(t)) {
      val nextQuantum = if (ingoingNeighboursFinished.isEmpty) {
        None
      } else {
        ingoingNeighboursFinished.map(_._2).reduce((maybeNextQuantum1, maybeNextQuantum2) => {
          //Find the minimum of two Option[Long]
          List(maybeNextQuantum1, maybeNextQuantum2).flatten match {
            case Nil => None
            case xs => Some(xs.min)
          } })
      }
      ingoingNeighboursFinished.clear()
      if (nextQuantum.isDefined){
        receiver.scheduleSimulationAdvance(nextQuantum.get)
        println(nextQuantum.get)
      } else {
        println("nothing?")
      }
    }
  }
}

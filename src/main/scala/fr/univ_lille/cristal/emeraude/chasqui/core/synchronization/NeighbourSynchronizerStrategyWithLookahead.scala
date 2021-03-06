package fr.univ_lille.cristal.emeraude.chasqui.core.synchronization

import akka.actor.ActorRef
import fr.univ_lille.cristal.emeraude.chasqui.core.Node.ScheduleMessage
import fr.univ_lille.cristal.emeraude.chasqui.core._

import scala.collection.mutable

/**
  * Created by guille on 29/05/17.
  */

case class FinishedQuantumWithLookahead(finishedQuantum: Long = -1, nextQuantum: Option[Long] = None) extends SynchronizationMessage {

  def hasPendingMessages(): Boolean = nextQuantum.isDefined
  def getFinishedQuantum(): Long = finishedQuantum
  def hasFinishedQuantum(t: Long): Boolean = {
    //We use finishedQuantum == -1 to mark that the status is "not-started"
    //TODO: Fix this, make a strategy
    this.finishedQuantum != -1 &&
      (this.getFinishedQuantum() >= t || !this.hasPendingMessages())
  }
}

class NeighbourSynchronizerStrategyWithLookahead() extends SynchronizerStrategy {

  private val messageQueue = scala.collection.mutable.PriorityQueue[Message]()(Ordering.fromLessThan((s1, s2) => s1.getTimestamp > s2.getTimestamp))
  val finishedTs = new mutable.HashSet[Long]()
  val ingoingNeighboursFinished: mutable.HashMap[ActorRef, FinishedQuantumWithLookahead] = new mutable.HashMap[ActorRef, FinishedQuantumWithLookahead]()

  override def registerNode(node: Node): Unit = {
    //Do nothing
    //We do not track any node
  }

  def statusOf(node: ActorRef): FinishedQuantumWithLookahead = {
    ingoingNeighboursFinished.getOrElse(node, new FinishedQuantumWithLookahead())
  }

  def notifyFinishedTime(nodeActor: ActorRef, node: Node, t: Long, messageDelta: Int): Unit = {

    if (finishedTs.contains(t)){
      //BUG, I'm finishing twice the same t, I'm going to notify it twice!
      val tuple1 = 1
    }
    finishedTs.add(t)

    node.broadcastMessageToOutgoing(FinishedQuantumWithLookahead(t, node.getRealIncomingQuantum), t)
    this.checkAdvanceSimulation(node, t)
  }

  def handleSynchronizationMessage(message: SynchronizationMessage, sender: ActorRef, receiver: Node, t: Long): Unit = {
    ingoingNeighboursFinished.update(sender, message.asInstanceOf[FinishedQuantumWithLookahead])
    this.checkAdvanceSimulation(receiver, t)
  }

  private def checkAdvanceSimulation(receiver: Node, t: Long): Unit = {
    // All neighbours are finished with current quantum if:
    // - either they announced they finished and that they have no more pending messages (t=None)
    // - or they announced they finished a t >= current node simulation time
    val allNeighboursAreFinishedWithQuantum =
      receiver.getIngoingConnections.forall(node => this.statusOf(node).hasFinishedQuantum(t))

    if (allNeighboursAreFinishedWithQuantum && !this.hasPendingMessagesOfTimestamp(t)) {
      val nextQuantum = (List(receiver.getRealIncomingQuantum) ++ ingoingNeighboursFinished.values.map(_.nextQuantum))
          .reduce((maybeNextQuantum1, maybeNextQuantum2) => {
            //Find the minimum of two Option[Long]
            List(maybeNextQuantum1, maybeNextQuantum2).flatten match {
              case Nil => None
              case xs => Some(xs.min)
            } })
      if (nextQuantum.isDefined){
        receiver.scheduleSimulationAdvance(nextQuantum.get)
      }
    }
  }

  def hasPendingMessagesOfTimestamp(t: Long) = {
    this.messageQueue.nonEmpty && this.messageQueue.head.getTimestamp == t
  }

  override def sendMessage(senderNode: Node, receiverActor: ActorRef, messageTimestamp: Long, message: Any): Unit = {
    receiverActor ! ScheduleMessage(message, messageTimestamp, senderNode.getActorRef)
  }

  override def scheduleMessage(receiverNode: Node, senderActor: ActorRef, messageTimestamp: Long, message: Any): Unit = {
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
      this.queueMessage(senderActor, messageTimestamp, message)
    }
    receiverNode.notifyFinishedQuantum()
  }

  private def queueMessage(senderActor: ActorRef, messageTimestamp: Long, message: Any) = {
    messageQueue += new Message(message, messageTimestamp, senderActor)
  }

  override def getMessageQueue: mutable.PriorityQueue[Message] = this.messageQueue
}

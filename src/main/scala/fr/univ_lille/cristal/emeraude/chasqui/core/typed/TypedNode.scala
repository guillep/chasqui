package fr.univ_lille.cristal.emeraude.chasqui.core.typed

import akka.Done
import akka.actor.ActorRef
import akka.util.Timeout
import akka.pattern.ask

import scala.concurrent.duration._
import fr.univ_lille.cristal.emeraude.chasqui.core.{CausalityErrorStrategy, Message, Node, SynchronizerStrategy}

import scala.collection.{Set, mutable}
import scala.concurrent.{Await, Future}

/**
  * Created by guille on 12/06/17.
  */
class TypedNode(val actor: ActorRef) {
  import Node._
  implicit val timeout = Timeout(21474835 seconds)

  def setId(id: String): Unit = {
    actor ! SetId(id)
  }

  def getIngoingConnections: Set[ActorRef] = {
    Await.result(actor ? GetIngoingConnections, Timeout(21474835 seconds).duration).asInstanceOf[Set[ActorRef]]
  }

  def getIngoingConnections(role: String): Set[ActorRef] = {
    Await.result(actor ? GetIngoingConnections(role), Timeout(21474835 seconds).duration).asInstanceOf[Set[ActorRef]]
  }

  def getOutgoingConnections: Set[ActorRef] = {
    Await.result(actor ? GetOutgoingConnections, Timeout(21474835 seconds).duration).asInstanceOf[Set[ActorRef]]
  }

  def getMessageTransferDeltaInCurrentQuantum(): Future[Int] = {
    (actor ? GetMessageTransferDeltaInCurrentQuantum).asInstanceOf[Future[Int]]
  }

  def blockingConnectTo(node: TypedNode, role: String): Done = {
    Await.result(actor ? ConnectTo(node.actor, role), Timeout(21474835 seconds).duration).asInstanceOf[Done]
  }

  def connectTo(node: TypedNode, role: String = "default"): Unit = {
    actor ! ConnectTo(node.actor, role)
  }

  def addIngoingConnectionTo(node: TypedNode, role: String): Unit = {
    actor ! AddIngoingConnectionTo(node.actor, role)
  }

  def receiveMessage(message: Any, sender: ActorRef): Unit = {
    actor ! ReceiveMessage(message, sender)
  }

  def setTime(t: Long): Unit = {
    actor ! SetTime(t)
  }

  def getCurrentSimulationTime(): Long = {
    Await.result(actor ? GetCurrentSimulationTime, Timeout(21474835 seconds).duration).asInstanceOf[Long]
  }

  def setSynchronizerStrategy(synchronizerStrategy: SynchronizerStrategy): Unit = {
    actor ! SetSynchronizerStrategy(synchronizerStrategy)
  }

  def checkPendingMessagesInQueue(): Unit = {
    actor ! CheckPendingMessagesInQueue
  }

  def processNextMessage(): Unit = {
    actor ! ProcessNextMessage
  }

  def processNextQuantum(): Unit = {
    actor ! ProcessNextQuantum
  }

  def notifyFinishedQuantum(): Unit = {
    actor ! NotifyFinishedQuantum
  }

  def getRealIncomingQuantum(): Option[Long] = {
    Await.result(this.getIncomingQuantum(), Timeout(21474835 seconds).duration)
  }

  def getIncomingQuantum(): Future[Option[Long]] = {
    (actor ? GetIncomingQuantum).asInstanceOf[Future[Option[Long]]]
  }

  def advanceSimulationTime(): Unit = {
    actor ! AdvanceSimulationTime
  }

  def advanceSimulationTime(nextQuantum: Long): Unit = {
    actor ! AdvanceSimulationTime(nextQuantum)
  }

  def scheduleSimulationAdvance(nextQuantum: Long): Unit = {
    this.advanceSimulationTime(nextQuantum)
  }

  def setCausalityErrorStrategy(causalityErrorStrategy: CausalityErrorStrategy): Unit = {
    actor ! SetCausalityErrorStrategy(causalityErrorStrategy)
  }

  def getScheduledMessages: mutable.PriorityQueue[Message] = {
    Await.result(actor ? GetScheduledMessages, Timeout(21474835 seconds).duration).asInstanceOf[mutable.PriorityQueue[Message]]
  }

  def hasPendingMessages(): Boolean = {
    Await.result(actor ? HasPendingMessages, Timeout(21474835 seconds).duration).asInstanceOf[Boolean]
  }

  def hasPendingMessagesOfTimestamp(t: Long): Boolean = {
    Await.result(actor ? HasPendingMessagesOfTimestamp(t), Timeout(21474835 seconds).duration).asInstanceOf[Boolean]
  }

  def broadcastMessageToIncoming(message: Any, timestamp: Long): Unit = {
    actor ! BroadcastMessageToIncoming(message, timestamp)
  }

  def sendMessage(receiver: ActorRef, timestamp: Long, message: Any): Any = {
    actor ! SendMessage(receiver, message, timestamp)
  }

  def scheduleMessage(message: Any, timestamp: Long, node: TypedNode): Unit = {
    this.scheduleMessage(message, timestamp, node.actor)
  }

  def scheduleMessage(message: Any, timestamp: Long, sender: ActorRef): Unit = {
    actor ! ScheduleMessage(message, timestamp, sender)
  }

  override def equals(obj: scala.Any): Boolean = {
    obj.asInstanceOf[TypedNode].actor == actor
  }

  override def hashCode(): Int = actor.hashCode()
}

package fr.univ_lille.cristal.emeraude.chasqui.core

import java.util.UUID

import akka.actor.TypedActor

import scala.collection.{Set, mutable}
import scala.concurrent.Future

/**
  * Created by guille on 10/04/17.
  */
trait Messaging {
  def sendMessage(receiver: Messaging, timestamp: Long, message: Any): Any
  def scheduleMessage(message: Any, timestamp: Long, sender: Messaging): Unit
}

case class IsReady(t: Long)
object PendingMessagesInQuantum
object NextQuantum
object CurrentQuantum

trait Node extends Messaging {
  def getId: String

  def setId(id: String)

  def self: Node

  def getIngoingConnections: Set[Node]

  def getIngoingConnections(role: String): Set[Node]

  def getOutgoingConnections: Set[Node]

  def getMessageTransferDeltaInCurrentQuantum(): Future[Int]

  def blockingConnectTo(node: Node, role: String = "default"): Option[_]

  def connectTo(node: Node, role: String = "default"): Unit

  def addIngoingConnectionTo(node: Node, role: String): Unit

  def receiveMessage(message: Any, sender: Messaging): Unit

  def setTime(t: Long): Unit

  def getCurrentSimulationTime(): Long

  def setSynchronizerStrategy(synchronizerStrategy: SynchronizerStrategy): Unit

  def checkPendingMessagesInQueue(): Unit

  def getRealIncomingQuantum(): Option[Long]

  def getIncomingQuantum(): Future[Option[Long]]

  def advanceSimulationTime(): Unit

  def advanceSimulationTime(nextQuantum: Long): Unit

  def setCausalityErrorStrategy(causalityErrorStrategy: CausalityErrorStrategy): Unit

  def getScheduledMessages: mutable.PriorityQueue[Message]

  def isReady: Future[Boolean] = Future.successful(true)

  def hasPendingMessages(): Boolean

  def broadcastMessageToIncoming(message: Any, timestamp: Long): Unit

}

object NullNode extends Messaging {
  override def sendMessage(receiver: Messaging, timestamp: Long, message: Any): Any = {
    throw new UnsupportedOperationException("Message cannot be sent to a null node")
  }

  override def scheduleMessage(message: Any, timestamp: Long, sender: Messaging): Unit = {
    throw new UnsupportedOperationException("Message cannot be scheduled into a null node")
  }
}

abstract class NodeImpl(private var causalityErrorStrategy : CausalityErrorStrategy = new ErrorCausalityErrorStrategy) extends Node {

  protected var id = UUID.randomUUID().toString

  def getId = id
  def setId(id: String) = this.id = id

  private var synchronizerStrategy: SynchronizerStrategy = new ManualSynchronizerStrategy
  private var currentSimulationTime: Long = 0
  private val messageQueue = scala.collection.mutable.PriorityQueue[Message]()(Ordering.fromLessThan((s1, s2) => s1.getTimestamp > s2.getTimestamp))

  private val outgoingConnections = new scala.collection.mutable.HashMap[Node, String]()
  private val outgoingConnectionsByRole = new scala.collection.mutable.HashMap[String, mutable.HashSet[Node]]()
  private val ingoingConnections = new scala.collection.mutable.HashMap[Node, String]()
  private val ingoingConnectionsByRole = new scala.collection.mutable.HashMap[String, mutable.HashSet[Node]]()

  private var sentMessagesInQuantum = 0
  private var receivedMessagesInQuantum = 0

  private var isStateReady = false

  def self: Node = TypedActor.self
  implicit val context = TypedActor.dispatcher

  def connectTo(node: Node, role: String="default"): Unit = {
    this.outgoingConnections += (node -> role)
    if (!this.outgoingConnectionsByRole.contains(role)){
      this.outgoingConnectionsByRole += (role -> new mutable.HashSet[Node]())
    }
    this.outgoingConnectionsByRole(role) += node

    this.manageOutgoingConnectionTo(node, role)
    node.addIngoingConnectionTo(self, role)
  }

  def blockingConnectTo(node: Node, role: String="default"): Option[_] = {
    this.connectTo(node, role)
    Some(true)
  }

  protected def manageOutgoingConnectionTo(node: Node, role: String): Unit ={
    //Hook for subclasses
    //Do nothing by default
  }

  def addIngoingConnectionTo(node: Node, role: String): Unit = {
    this.ingoingConnections += (node -> role)
    if (!this.ingoingConnectionsByRole.contains(role)){
      this.ingoingConnectionsByRole += (role -> new mutable.HashSet[Node]())
    }
    this.ingoingConnectionsByRole(role) += node

    this.manageIngoingConnectionFrom(node)
  }

  protected def manageIngoingConnectionFrom(node: Node): Unit ={
    //Hook for subclasses
    //Do nothing by default
  }

  def getOutgoingConnections: Set[Node] = this.outgoingConnections.keySet
  def getIngoingConnections: Set[Node] = this.ingoingConnections.keySet
  def getMessageTransferDeltaInCurrentQuantum(): Future[Int] = Future.successful(getMessageDeltaInQuantum)

  def getOutgoingConnectionsWithRole(role: String) = {
    this.outgoingConnectionsByRole.getOrElse(role, new mutable.HashSet[Node]())
  }

  override def getIngoingConnections(role: String): Set[Node] = {
    this.ingoingConnectionsByRole.getOrElse(role, new mutable.HashSet[Node]())
  }

  def ingoingConnectionsDo(role: String, function: (Node) => Unit): Unit = {
    if (!this.ingoingConnectionsByRole.contains(role)) {
      return
    }
    this.ingoingConnectionsByRole(role).foreach(function)
  }


  def setTime(t: Long): Unit = {
    this.currentSimulationTime = t
  }
  def getCurrentSimulationTime(): Long = this.currentSimulationTime

  def setSynchronizerStrategy(synchronizerStrategy: SynchronizerStrategy): Unit ={
    this.synchronizerStrategy = synchronizerStrategy
    this.synchronizerStrategy.registerNode(self)
  }

  def advanceSimulationTime(): Unit = {
    this.advanceSimulationTime(this.currentSimulationTime + 1)
  }

  def advanceSimulationTime(nextQuantum: Long): Unit = {
    this.currentSimulationTime = nextQuantum
    this.checkPendingMessagesInQueue()
  }

  def getIncomingQuantum(): Future[Option[Long]] = {
    Future.successful(this.getRealIncomingQuantum())
  }

  def getRealIncomingQuantum(): Option[Long] = {
    if (this.messageQueue.isEmpty) { None }
    else {
      Some(this.messageQueue.head.getTimestamp)
    }
  }

  def checkPendingMessagesInQueue() = {
    //Get all elements in the same priority and remove them from the message queue
    //TODO: In a very big recursion this could create a stack overflow
    this.isStateReady = false
    val currentTimestampMessages = new mutable.Queue[Message]()
    while (this.messageQueue.nonEmpty && this.messageQueue.head.getTimestamp == this.currentSimulationTime) {
      currentTimestampMessages.enqueue(this.messageQueue.dequeue())
    }
    currentTimestampMessages.foreach(m => this.internalReceiveMessage(m.getMessage, m.getSender))

    this.sentMessagesInQuantum = 0
    this.receivedMessagesInQuantum = 0
    this.isStateReady = true
    this.synchronizerStrategy.notifyFinishedTime(self, this.currentSimulationTime, this.getScheduledMessages.size, this.getMessageDeltaInQuantum)
  }

  private def getMessageDeltaInQuantum = {
    this.sentMessagesInQuantum - this.receivedMessagesInQuantum
  }

  def queueMessage(message: Any, timestamp: Long, sender: Messaging): Unit = {
    messageQueue += new Message(message, timestamp, sender)
  }

  def getScheduledMessages: mutable.PriorityQueue[Message] = this.messageQueue

  def hasPendingMessages(): Boolean = {
    !this.messageQueue.isEmpty
  }

  def sendMessage(receiver: Messaging, timestamp: Long, message: Any): Any = {
    this.sentMessagesInQuantum += 1
    receiver.scheduleMessage(message, timestamp, self)
  }

  def broadcastMessage(timestamp: Long, message: Any, roleToBroadcastTo: String = "default"): Unit = {
    this.getOutgoingConnectionsWithRole(roleToBroadcastTo).foreach { node: Node =>
      this.sendMessage(node, timestamp, message)
    }
  }

  def broadcastMessageToIncoming(message: Any, timestamp: Long): Unit = {
    this.getIngoingConnections.foreach { node: Node =>
      this.sendMessage(node, timestamp, message)
    }
  }

  def scheduleMessage(message: Any, timestamp: Long, sender: Messaging): Unit = {

    this.receivedMessagesInQuantum += 1

    if (timestamp < this.currentSimulationTime) {
      //The message is in the past.
      //This is a Causality error
      causalityErrorStrategy.handleCausalityError(timestamp, this.currentSimulationTime, self, sender, message)
      return
    }

    if (this.currentSimulationTime == timestamp){
      this.handleIncomingMessage(message, sender)
    } else {
      this.queueMessage(message, timestamp, sender)
    }
  }

  def setCausalityErrorStrategy(causalityErrorStrategy: CausalityErrorStrategy): Unit = {
    this.causalityErrorStrategy = causalityErrorStrategy
  }

  protected def handleIncomingMessage(message: Any, sender: Messaging): Unit = {
    this.internalReceiveMessage(message, sender)
    this.checkPendingMessagesInQueue()
  }

  def internalReceiveMessage(message: Any, sender: Messaging): Unit = {
    try{
      if (message.isInstanceOf[SynchronizationMessage]){
        this.synchronizerStrategy.handleSynchronizationMessage(message.asInstanceOf[SynchronizationMessage], sender, self)
      }else {
        this.receiveMessage(message, sender)
      }
    }catch{
      case e => throw new UnhandledChasquiException(this, message, sender, this.getCurrentSimulationTime(), e)
    }
  }
  def receiveMessage(message: Any, sender: Messaging)
}

package fr.univ_lille.cristal.emeraude.chasqui.core

import java.util.UUID

import akka.Done
import akka.actor.{Actor, ActorRef}
import fr.univ_lille.cristal.emeraude.chasqui.core.Node._
import fr.univ_lille.cristal.emeraude.chasqui.core.causality.ErrorCausalityErrorStrategy
import fr.univ_lille.cristal.emeraude.chasqui.core.synchronization.ManualSynchronizerStrategy

import scala.collection.{Set, mutable}
import scala.concurrent.Future

/**
  * Created by guille on 10/04/17.
  */
trait Messaging {
  def sendMessage(receiver: ActorRef, timestamp: Long, message: Any): Any
  def scheduleMessage(message: Any, timestamp: Long, sender: ActorRef): Unit
}

object Node {

  object GetId
  case class SetId(id: String)

  object GetIngoingConnections
  case class GetIngoingConnections(role: String)
  object GetOutgoingConnections

  object GetMessageTransferDeltaInCurrentQuantum

  case class ConnectTo(actor: ActorRef, role: String = "default")
  case class AddIngoingConnectionTo(actor: ActorRef, role: String)

  case class ReceiveMessage(message: Any, sender: ActorRef)

  case class SetTime(t: Long)
  object GetCurrentSimulationTime

  case class SetCausalityErrorStrategy(causalityErrorStrategy: CausalityErrorStrategy)
  case class SetSynchronizerStrategy(strategy: SynchronizerStrategy)

  object Start
  object ProcessNextMessage
  object ProcessNextQuantum
  object CheckPendingMessagesInQueue

  object NotifyFinishedQuantum
  object GetIncomingQuantum
  //object AdvanceSimulationTime
  case class AdvanceSimulationTime(quantum: Long)

  object GetScheduledMessages
  case class BroadcastMessageToIncoming(message: Any, timestamp: Long)
  case class SendMessage(receiver: ActorRef, message: Any, timestamp: Long)
  case class ScheduleMessage(message: Any, timestamp: Long, sender: ActorRef)

  case class IsReady(t: Long)
  object PendingMessagesInQuantum
  object NextQuantum
  object CurrentQuantum

  /****************************************************************
    *
    * Queries API
    *
    *****************************************************************/

  object GetNodeSummary
}

trait Node extends Messaging {
  def setId(id: String)
  def getActorRef: ActorRef

  def getIngoingConnections: Set[ActorRef]

  def getIngoingConnections(role: String): Set[ActorRef]

  def getOutgoingConnections: Set[ActorRef]

  def getMessageTransferDeltaInCurrentQuantum: Future[Int]

  def receiveMessage(message: Any, sender: ActorRef): Unit

  def setTime(t: Long): Unit

  def getCurrentSimulationTime: Long
  def setSynchronizerStrategy(synchronizerStrategy: SynchronizerStrategy): Unit
  def processNextQuantum(): Unit
  def processNextMessage(): Unit
  def notifyFinishedQuantum(): Unit

  def getRealIncomingQuantum: Option[Long]
  def getIncomingQuantum: Future[Option[Long]]

  def advanceSimulationTime(nextQuantum: Long): Unit
  def scheduleSimulationAdvance(nextQuantum: Long): Unit

  def setCausalityErrorStrategy(causalityErrorStrategy: CausalityErrorStrategy): Unit

  def getScheduledMessages: mutable.PriorityQueue[Message]

  def isReady: Future[Boolean] = Future.successful(true)

  def hasPendingMessages: Boolean

  def broadcastMessage(timestamp: Long, message: Any, roleToBroadcastTo: String = "default"): Unit
  def broadcastMessageToIncoming(message: Any, timestamp: Long): Unit
  def broadcastMessageToOutgoing(message: Any, timestamp: Long): Unit

}

object NullNode extends Messaging {
  override def sendMessage(receiver: ActorRef, timestamp: Long, message: Any): Any = {
    throw new UnsupportedOperationException("Message cannot be sent to a null node")
  }

  override def scheduleMessage(message: Any, timestamp: Long, sender: ActorRef): Unit = {
    throw new UnsupportedOperationException("Message cannot be scheduled into a null node")
  }
}

abstract class NodeImpl(private var causalityErrorStrategy : CausalityErrorStrategy = new ErrorCausalityErrorStrategy) extends Actor with Node {
  def log(s: String): Unit = {
    //TODO: implement logging system
    //println(s)
  }

  protected var id: String = UUID.randomUUID().toString
  def getId: String = id
  def setId(id: String): Unit = this.id = id

  protected var status = "Not started"
  def setStatus(s: String): Unit = this.status = s

  override def toString: String = super.toString + s"($id, $status)"

  def getActorRef: ActorRef = self

  // For debugging and testing purposes
  // Sometimes we want to force a node to not process messages as soon as it arrives to a new quantum
  // Otherwise, it can loop and consume all its message queue
  protected var automaticallyProcessQuantum: Boolean = true

  def doNotAutomaticallyProcessQuantum(): Node = {
    this.automaticallyProcessQuantum = false
    this
  }

  private var synchronizerStrategy: SynchronizerStrategy = new ManualSynchronizerStrategy
  private var currentSimulationTime: Long = 0

  private val outgoingConnections = new scala.collection.mutable.HashMap[ActorRef, String]()
  private val outgoingConnectionsByRole = new scala.collection.mutable.HashMap[String, mutable.HashSet[ActorRef]]()
  private val ingoingConnections = new scala.collection.mutable.HashMap[ActorRef, String]()
  private val ingoingConnectionsByRole = new scala.collection.mutable.HashMap[String, mutable.HashSet[ActorRef]]()

  private var sentMessagesInQuantum = 0
  private var receivedMessagesInQuantum = 0

  def connectTo(target: ActorRef, role: String="default"): Unit = {
    this.outgoingConnections += (target -> role)
    if (!this.outgoingConnectionsByRole.contains(role)){
      this.outgoingConnectionsByRole += (role -> new mutable.HashSet[ActorRef]())
    }
    this.outgoingConnectionsByRole(role) += target

    this.manageOutgoingConnectionTo(target, role)
    target ! AddIngoingConnectionTo(self, role)
  }

  protected def manageOutgoingConnectionTo(node: ActorRef, role: String): Unit ={
    //Hook for subclasses
    //Do nothing by default
  }

  def addIngoingConnectionTo(target: ActorRef, role: String): Unit = {
    this.ingoingConnections += (target -> role)
    if (!this.ingoingConnectionsByRole.contains(role)){
      this.ingoingConnectionsByRole += (role -> new mutable.HashSet[ActorRef]())
    }
    this.ingoingConnectionsByRole(role) += target

    this.manageIngoingConnectionFrom(target)
  }

  protected def manageIngoingConnectionFrom(node: ActorRef): Unit ={
    //Hook for subclasses
    //Do nothing by default
  }

  def getOutgoingConnections: Set[ActorRef] = this.outgoingConnections.keySet
  def getIngoingConnections: Set[ActorRef] = this.ingoingConnections.keySet
  def getMessageTransferDeltaInCurrentQuantum: Future[Int] = Future.successful(getMessageDeltaInQuantum)

  def getOutgoingConnectionsWithRole(role: String): Set[ActorRef] = {
    this.outgoingConnectionsByRole.getOrElse(role, new mutable.HashSet[ActorRef]())
  }

  def getIngoingConnections(role: String): Set[ActorRef] = {
    this.ingoingConnectionsByRole.getOrElse(role, new mutable.HashSet[ActorRef]())
  }

  def ingoingConnectionsDo(role: String, function: (ActorRef) => Unit): Unit = {
    if (!this.ingoingConnectionsByRole.contains(role)) {
      return
    }
    this.ingoingConnectionsByRole(role).foreach(function)
  }


  def setTime(t: Long): Unit = {
    this.currentSimulationTime = t
  }
  def getCurrentSimulationTime: Long = this.currentSimulationTime

  def setSynchronizerStrategy(synchronizerStrategy: SynchronizerStrategy): Unit = {
    this.synchronizerStrategy = synchronizerStrategy
    this.synchronizerStrategy.registerNode(this)
  }

  def advanceSimulationTime(nextQuantum: Long): Unit = {
    if (nextQuantum < this.currentSimulationTime) {
      throw new UnsupportedOperationException(s"Cannot advance to previous quantum $nextQuantum. Current quantum is $currentSimulationTime")
    }
    this.currentSimulationTime = nextQuantum
    this.sentMessagesInQuantum = 0
    this.receivedMessagesInQuantum = 0
  }

  def scheduleSimulationAdvance(nextQuantum: Long): Unit = {
    self ! AdvanceSimulationTime(nextQuantum)
    self ! ProcessNextQuantum
  }

  def getIncomingQuantum: Future[Option[Long]] = {
    Future.successful(this.getRealIncomingQuantum)
  }

  def getRealIncomingQuantum: Option[Long] = {
    if (this.getMessageQueue.isEmpty) { None }
    else {
      Some(this.getMessageQueue.head.getTimestamp)
    }
  }

  /****************************************************************************
  *  Checking messages
  ****************************************************************************/

  def getMessageQueue = this.synchronizerStrategy.getMessageQueue

  /**
    * Get all elements in the same priority and remove them from the message queue
    * TODO: In a very big recursion this could create a stack overflow
    */
  def processNextQuantum(): Unit = {
    while (this.hasMessagesForThisQuantum) {
      this.uncheckedProcessNextMessage()
    }
    this.notifyFinishedQuantum()
  }

  def hasMessagesForThisQuantum: Boolean = {
    this.getMessageQueue.nonEmpty && this.getMessageQueue.head.getTimestamp == this.currentSimulationTime
  }

  def uncheckedProcessNextMessage(): Unit = {
    this.setStatus(s"Processing messages in t=$currentSimulationTime")
    val message = this.getMessageQueue.dequeue()
    this.internalReceiveMessage(message.getMessage, message.getSender)
  }

  def processNextMessage(): Unit = {
    if (this.getMessageQueue.isEmpty) {
      throw new UnsupportedOperationException("No more messages to process")
    }
    this.uncheckedProcessNextMessage()
    this.verifyFinishedQuantum()
  }

  private def verifyFinishedQuantum() = {
    if (this.currentSimulationTime < this.getMessageQueue.head.getTimestamp) {
      this.notifyFinishedQuantum()
    }
  }

  def notifyFinishedQuantum(): Unit = {
    this.setStatus(s"Waiting for quantum to finish t=$currentSimulationTime")
    this.synchronizerStrategy.notifyFinishedTime(self, this, this.currentSimulationTime, this.getScheduledMessages.size, this.getMessageDeltaInQuantum)
  }

  private def getMessageDeltaInQuantum: Int = {
    this.sentMessagesInQuantum - this.receivedMessagesInQuantum
  }

  def getScheduledMessages: mutable.PriorityQueue[Message] = this.getMessageQueue

  def sendMessage(receiver: ActorRef, timestamp: Long, message: Any): Any = {
    this.log(s"| sent | $self | $receiver| $currentSimulationTime | $message |")
    this.synchronizerStrategy.sendMessage(this, receiver, timestamp, message)
  }

  def broadcastMessage(timestamp: Long, message: Any, roleToBroadcastTo: String = "default"): Unit = {
    this.getOutgoingConnectionsWithRole(roleToBroadcastTo).foreach { node: ActorRef =>
      this.sendMessage(node, timestamp, message)
    }
  }

  def broadcastMessageToOutgoing(message: Any, timestamp: Long): Unit = {
    this.getOutgoingConnections.foreach { node: ActorRef =>
      this.sendMessage(node, timestamp, message)
    }
  }

  def broadcastMessageToIncoming(message: Any, timestamp: Long): Unit = {
    this.getIngoingConnections.foreach { node: ActorRef =>
      this.sendMessage(node, timestamp, message)
    }
  }

  def scheduleMessage(message: Any, timestamp: Long, senderActorRef: ActorRef): Unit = {
    this.log(s"| received | $senderActorRef | $self | $currentSimulationTime | $message |")
    this.synchronizerStrategy.scheduleMessage(this, senderActorRef, timestamp, message)
  }

  /**
    * Starts a simulation:
    * Process the current node quantum and wait for synchronization instructions
    */
  def start(): Unit = {
    assert(this.currentSimulationTime == 0)
    assert(this.receivedMessagesInQuantum == 0)
    assert(this.sentMessagesInQuantum == 0)
    this.scheduleSimulationAdvance(this.getCurrentSimulationTime)
  }

  def getCausalityErrorStrategy = this.causalityErrorStrategy
  def setCausalityErrorStrategy(causalityErrorStrategy: CausalityErrorStrategy): Unit = {
    this.causalityErrorStrategy = causalityErrorStrategy
  }

  def handleIncomingMessage(message: Any, sender: ActorRef): Unit = {
    this.internalReceiveMessage(message, sender)
    //this.checkPendingMessagesInQueue()
  }

  def internalReceiveMessage(message: Any, sender: ActorRef): Unit = {
    try{
      message match {
        case synchronizationMessage: SynchronizationMessage =>
          this.synchronizerStrategy.handleSynchronizationMessage(synchronizationMessage, sender, this, this.getCurrentSimulationTime)
        case _ =>
          this.receiveMessage(message, sender)
      }
    }catch{
      case e: Throwable => throw new UnhandledChasquiException(this, message, sender, this.getCurrentSimulationTime, e)
    }
  }
  def receiveMessage(message: Any, sender: ActorRef)

  override def receive: Receive = {
    case GetId => sender ! this.getId
    case SetId(id) => this.setId(id)
    case GetIngoingConnections => sender ! this.getIngoingConnections
    case GetIngoingConnections(role) => sender ! this.getIngoingConnections(role)
    case GetOutgoingConnections => sender ! this.getOutgoingConnections
    case GetMessageTransferDeltaInCurrentQuantum => sender ! this.getMessageDeltaInQuantum
    case ConnectTo(node, role) =>
      this.connectTo(node, role)
      // For the blocking counterpart
      // This will not really work because this message will dispatch a second asynchronous message
      // to ${node} that will not wait
      sender ! Done
    case AddIngoingConnectionTo(actor, role) =>
      this.addIngoingConnectionTo(actor, role)
    case ReceiveMessage(message, sender) => this.receiveMessage(message, sender)
    case SetTime(time) => this.setTime(time)
    case GetCurrentSimulationTime => sender ! this.getCurrentSimulationTime
    case SetSynchronizerStrategy(strategy) => this.setSynchronizerStrategy(strategy)

    case Start => this.start()
    case ProcessNextMessage => this.processNextMessage()
    case ProcessNextQuantum => this.processNextQuantum()
    case CheckPendingMessagesInQueue => this.processNextQuantum()
    case NotifyFinishedQuantum => this.notifyFinishedQuantum()


    case GetIncomingQuantum => sender ! this.getRealIncomingQuantum
    //case AdvanceSimulationTime => this.advanceSimulationTime(this.getRealIncomingQuantum().get)
    case AdvanceSimulationTime(nextQuantum) =>
      this.advanceSimulationTime(nextQuantum)
      sender ! nextQuantum

    case SetCausalityErrorStrategy(strategy) => this.setCausalityErrorStrategy(strategy)
    case GetScheduledMessages => sender ! this.getScheduledMessages
    case BroadcastMessageToIncoming(message, timestamp) => this.broadcastMessage(timestamp, message)
    case SendMessage(receiver, message, timestamp) => this.sendMessage(receiver, timestamp, message)

    case ScheduleMessage(message, timestamp, sender) => this.scheduleMessage(message, timestamp, sender)

    case GetNodeSummary => sender ! (id, status, currentSimulationTime, getMessageQueue.toSeq, this.getMessageDeltaInQuantum)
  }
}

package fr.univ_lille.cristal.emeraude.chasqui.core

import java.util.UUID

import akka.actor.{Actor, ActorRef}
import akka.pattern.ask
import akka.util.Timeout
import fr.univ_lille.cristal.emeraude.chasqui.core.Node._
import fr.univ_lille.cristal.emeraude.chasqui.core.causality.ErrorCausalityErrorStrategy
import fr.univ_lille.cristal.emeraude.chasqui.core.synchronization.ManualSynchronizerStrategy

import scala.collection.{Set, mutable}
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by guille on 10/04/17.
  */
trait Messaging {
  def sendMessage(receiver: ActorRef, timestamp: Long, message: Any): Any
  def scheduleMessage(message: Any, timestamp: Long, sender: ActorRef): Unit
}

object Node {
  case class SetId(id: String)

  object GetIngoingConnections
  case class GetIngoingConnections(role: String)
  object GetOutgoingConnections

  object GetMessageTransferDeltaInCurrentQuantum

  case class ConnectTo(actor: ActorRef, role: String = "default")
  case class AddIngoingConnectionTo(actor: ActorRef, role: String)

  case class ReceiveMessage(message: Any, sender: Messaging)

  case class SetTime(t: Long)
  object GetCurrentSimulationTime

  case class SetCausalityErrorStrategy(causalityErrorStrategy: CausalityErrorStrategy)
  case class SetSynchronizerStrategy(strategy: SynchronizerStrategy)

  object CheckPendingMessagesInQueue
  object NotifyFinishedQuantum
  object GetIncomingQuantum
  object AdvanceSimulationTime
  case class AdvanceSimulationTime(quantum: Long)

  object GetScheduledMessages
  object HasPendingMessages
  case class HasPendingMessagesOfTimestamp(t: Long)
  case class BroadcastMessageToIncoming(message: Any, timestamp: Long)
  case class SendMessage(receiver: ActorRef, message: Any, timestamp: Long)
  case class ScheduleMessage(message: Any, timestamp: Long, sender: ActorRef)

  case class IsReady(t: Long)
  object PendingMessagesInQuantum
  object NextQuantum
  object CurrentQuantum
}

trait Node extends Messaging {
  def setId(id: String)

  def getIngoingConnections: Set[NodeActorWrapper]

  def getIngoingConnections(role: String): Set[NodeActorWrapper]

  def getOutgoingConnections: Set[NodeActorWrapper]

  def getMessageTransferDeltaInCurrentQuantum(): Future[Int]

  def blockingConnectTo(node: NodeActorWrapper, role: String = "default"): Option[_]

  def connectTo(node: NodeActorWrapper, role: String = "default"): Unit

  def addIngoingConnectionTo(node: NodeActorWrapper, role: String): Unit

  def receiveMessage(message: Any, sender: Messaging): Unit

  def setTime(t: Long): Unit

  def getCurrentSimulationTime(): Long
  def setSynchronizerStrategy(synchronizerStrategy: SynchronizerStrategy): Unit
  def checkPendingMessagesInQueue(): Unit
  def notifyFinishedQuantum(): Unit

  def getRealIncomingQuantum(): Option[Long]
  def getIncomingQuantum(): Future[Option[Long]]

  def advanceSimulationTime(): Unit
  def advanceSimulationTime(nextQuantum: Long): Unit
  def scheduleSimulationAdvance(nextQuantum: Long): Unit

  def setCausalityErrorStrategy(causalityErrorStrategy: CausalityErrorStrategy): Unit

  def getScheduledMessages: mutable.PriorityQueue[Message]

  def isReady: Future[Boolean] = Future.successful(true)

  def hasPendingMessages(): Boolean

  def hasPendingMessagesOfTimestamp(t: Long): Boolean

  def broadcastMessageToIncoming(message: Any, timestamp: Long): Unit

}

object NullNode extends Messaging {
  override def sendMessage(receiver: ActorRef, timestamp: Long, message: Any): Any = {
    throw new UnsupportedOperationException("Message cannot be sent to a null node")
  }

  override def scheduleMessage(message: Any, timestamp: Long, sender: ActorRef): Unit = {
    throw new UnsupportedOperationException("Message cannot be scheduled into a null node")
  }
}

class NodeActorWrapper(val actor: ActorRef) extends Node {
  import Node._
  implicit val timeout = Timeout(21474835 seconds)

  override def setId(id: String): Unit = {
    actor ! SetId(id)
  }

  override def getIngoingConnections: Set[NodeActorWrapper] = {
    Await.result(actor ? GetIngoingConnections, Timeout(21474835 seconds).duration).asInstanceOf[Set[NodeActorWrapper]]
  }

  override def getIngoingConnections(role: String): Set[NodeActorWrapper] = {
    Await.result(actor ? GetIngoingConnections(role), Timeout(21474835 seconds).duration).asInstanceOf[Set[NodeActorWrapper]]
  }

  override def getOutgoingConnections: Set[NodeActorWrapper] = {
    Await.result(actor ? GetOutgoingConnections, Timeout(21474835 seconds).duration).asInstanceOf[Set[NodeActorWrapper]]
  }

  override def getMessageTransferDeltaInCurrentQuantum(): Future[Int] = {
    (actor ? GetMessageTransferDeltaInCurrentQuantum).asInstanceOf[Future[Int]]
  }

  override def blockingConnectTo(node: NodeActorWrapper, role: String): Option[_] = {
    Await.result(actor ? GetOutgoingConnections, Timeout(21474835 seconds).duration).asInstanceOf[Option[_]]
  }

  override def connectTo(node: NodeActorWrapper, role: String): Unit = {
    actor ! ConnectTo(node.actor, role)
  }

  override def addIngoingConnectionTo(node: NodeActorWrapper, role: String): Unit = {
    actor ! AddIngoingConnectionTo(node.actor, role)
  }

  override def receiveMessage(message: Any, sender: Messaging): Unit = {
    actor ! ReceiveMessage(message, sender)
  }

  override def setTime(t: Long): Unit = {
    actor ! SetTime(t)
  }

  override def getCurrentSimulationTime(): Long = {
    Await.result(actor ? GetCurrentSimulationTime, Timeout(21474835 seconds).duration).asInstanceOf[Long]
  }

  override def setSynchronizerStrategy(synchronizerStrategy: SynchronizerStrategy): Unit = {
    actor ! SetSynchronizerStrategy(synchronizerStrategy)
  }

  override def checkPendingMessagesInQueue(): Unit = {
    actor ! CheckPendingMessagesInQueue
  }

  override def notifyFinishedQuantum(): Unit = {
    actor ! NotifyFinishedQuantum
  }

  override def getRealIncomingQuantum(): Option[Long] = {
    Await.result(this.getIncomingQuantum(), Timeout(21474835 seconds).duration)
  }

  override def getIncomingQuantum(): Future[Option[Long]] = {
    (actor ? GetIncomingQuantum).asInstanceOf[Future[Option[Long]]]
  }

  override def advanceSimulationTime(): Unit = {
    actor ! AdvanceSimulationTime
  }

  override def advanceSimulationTime(nextQuantum: Long): Unit = {
    actor ! AdvanceSimulationTime(nextQuantum)
  }

  override def scheduleSimulationAdvance(nextQuantum: Long): Unit = {
    this.advanceSimulationTime(nextQuantum)
  }

  override def setCausalityErrorStrategy(causalityErrorStrategy: CausalityErrorStrategy): Unit = {
    actor ! SetCausalityErrorStrategy(causalityErrorStrategy)
  }

  override def getScheduledMessages: mutable.PriorityQueue[Message] = {
    Await.result(actor ? GetScheduledMessages, Timeout(21474835 seconds).duration).asInstanceOf[mutable.PriorityQueue[Message]]
  }

  override def hasPendingMessages(): Boolean = {
    Await.result(actor ? HasPendingMessages, Timeout(21474835 seconds).duration).asInstanceOf[Boolean]
  }

  override def hasPendingMessagesOfTimestamp(t: Long): Boolean = {
    Await.result(actor ? HasPendingMessagesOfTimestamp(t), Timeout(21474835 seconds).duration).asInstanceOf[Boolean]
  }

  override def broadcastMessageToIncoming(message: Any, timestamp: Long): Unit = {
    actor ! BroadcastMessageToIncoming(message, timestamp)
  }

  override def sendMessage(receiver: ActorRef, timestamp: Long, message: Any): Any = {
    actor ! SendMessage(receiver, message, timestamp)
  }

  override def scheduleMessage(message: Any, timestamp: Long, sender: ActorRef): Unit = {
    actor ! ScheduleMessage(message, timestamp, sender)
  }

  override def equals(obj: scala.Any): Boolean = {
    obj.asInstanceOf[NodeActorWrapper].actor == actor
  }

  override def hashCode(): Int = actor.hashCode()
}

abstract class NodeImpl(private var causalityErrorStrategy : CausalityErrorStrategy = new ErrorCausalityErrorStrategy) extends Actor with Node {

  protected var id = UUID.randomUUID().toString

  def getId = id
  def setId(id: String) = this.id = id

  override def toString = super.toString + s"(${this.id})"

  private val wrapper = new NodeActorWrapper(self)

  private var synchronizerStrategy: SynchronizerStrategy = new ManualSynchronizerStrategy
  private var currentSimulationTime: Long = 0
  private val messageQueue = scala.collection.mutable.PriorityQueue[Message]()(Ordering.fromLessThan((s1, s2) => s1.getTimestamp > s2.getTimestamp))

  private val outgoingConnections = new scala.collection.mutable.HashMap[NodeActorWrapper, String]()
  private val outgoingConnectionsByRole = new scala.collection.mutable.HashMap[String, mutable.HashSet[NodeActorWrapper]]()
  private val ingoingConnections = new scala.collection.mutable.HashMap[NodeActorWrapper, String]()
  private val ingoingConnectionsByRole = new scala.collection.mutable.HashMap[String, mutable.HashSet[NodeActorWrapper]]()

  private var sentMessagesInQuantum = 0
  private var receivedMessagesInQuantum = 0

  def connectTo(node: NodeActorWrapper, role: String="default"): Unit = {
    this.outgoingConnections += (node -> role)
    if (!this.outgoingConnectionsByRole.contains(role)){
      this.outgoingConnectionsByRole += (role -> new mutable.HashSet[NodeActorWrapper]())
    }
    this.outgoingConnectionsByRole(role) += node

    this.manageOutgoingConnectionTo(node, role)
    node.addIngoingConnectionTo(wrapper, role)
  }

  def blockingConnectTo(node: NodeActorWrapper, role: String="default"): Option[_] = {
    this.connectTo(node, role)
    Some(true)
  }

  protected def manageOutgoingConnectionTo(node: NodeActorWrapper, role: String): Unit ={
    //Hook for subclasses
    //Do nothing by default
  }

  def addIngoingConnectionTo(node: NodeActorWrapper, role: String): Unit = {
    this.ingoingConnections += (node -> role)
    if (!this.ingoingConnectionsByRole.contains(role)){
      this.ingoingConnectionsByRole += (role -> new mutable.HashSet[NodeActorWrapper]())
    }
    this.ingoingConnectionsByRole(role) += node

    this.manageIngoingConnectionFrom(node)
  }

  protected def manageIngoingConnectionFrom(node: NodeActorWrapper): Unit ={
    //Hook for subclasses
    //Do nothing by default
  }

  def getOutgoingConnections: Set[NodeActorWrapper] = this.outgoingConnections.keySet
  def getIngoingConnections: Set[NodeActorWrapper] = this.ingoingConnections.keySet
  def getMessageTransferDeltaInCurrentQuantum(): Future[Int] = Future.successful(getMessageDeltaInQuantum)

  def getOutgoingConnectionsWithRole(role: String): Set[NodeActorWrapper] = {
    this.outgoingConnectionsByRole.getOrElse(role, new mutable.HashSet[NodeActorWrapper]())
  }

  override def getIngoingConnections(role: String): Set[NodeActorWrapper] = {
    this.ingoingConnectionsByRole.getOrElse(role, new mutable.HashSet[NodeActorWrapper]())
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
    this.sentMessagesInQuantum = 0
    this.receivedMessagesInQuantum = 0
    this.checkPendingMessagesInQueue()
  }

  def scheduleSimulationAdvance(nextQuantum: Long): Unit = {
    wrapper.advanceSimulationTime(nextQuantum)
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

  /**
    * Get all elements in the same priority and remove them from the message queue
    * TODO: In a very big recursion this could create a stack overflow
    */
  def checkPendingMessagesInQueue() = {
    val currentTimestampMessages = new mutable.Queue[Message]()
    while (this.messageQueue.nonEmpty && this.messageQueue.head.getTimestamp == this.currentSimulationTime) {
      currentTimestampMessages.enqueue(this.messageQueue.dequeue())
    }
    currentTimestampMessages.foreach(m => this.internalReceiveMessage(m.getMessage, m.getSender))

    this.notifyFinishedQuantum()
  }

  def notifyFinishedQuantum(): Unit = {
    this.synchronizerStrategy.notifyFinishedTime(self, this, this.currentSimulationTime, this.getScheduledMessages.size, this.getMessageDeltaInQuantum)
  }

  private def getMessageDeltaInQuantum: Int = {
    this.sentMessagesInQuantum - this.receivedMessagesInQuantum
  }

  def queueMessage(message: Any, timestamp: Long, sender: Messaging): Unit = {
    messageQueue += new Message(message, timestamp, sender)
  }

  def getScheduledMessages: mutable.PriorityQueue[Message] = this.messageQueue

  def hasPendingMessages(): Boolean = this.messageQueue.nonEmpty

  def hasPendingMessagesOfTimestamp(t: Long): Boolean = {
    this.hasPendingMessages() && this.messageQueue.head.getTimestamp == t
  }

  def sendMessage(receiver: ActorRef, timestamp: Long, message: Any): Any = {
    this.sentMessagesInQuantum += 1
    new NodeActorWrapper(receiver).scheduleMessage(message, timestamp, self)
  }

  def broadcastMessage(timestamp: Long, message: Any, roleToBroadcastTo: String = "default"): Unit = {
    this.getOutgoingConnectionsWithRole(roleToBroadcastTo).foreach { node: NodeActorWrapper =>
      this.sendMessage(node.actor, timestamp, message)
    }
  }

  def broadcastMessageToIncoming(message: Any, timestamp: Long): Unit = {
    this.getIngoingConnections.foreach { node: NodeActorWrapper =>
      this.sendMessage(node.actor, timestamp, message)
    }
  }

  def scheduleMessage(message: Any, timestamp: Long, senderActorRef: ActorRef): Unit = {

    val sender = new NodeActorWrapper(senderActorRef)
    this.receivedMessagesInQuantum += 1

    if (timestamp < this.currentSimulationTime) {
      //The message is in the past.
      //This is a Causality error
      causalityErrorStrategy.handleCausalityError(timestamp, this.currentSimulationTime, wrapper, sender, message)
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
    //this.checkPendingMessagesInQueue()
  }

  def internalReceiveMessage(message: Any, sender: Messaging): Unit = {
    try{
      if (message.isInstanceOf[SynchronizationMessage]){
        this.synchronizerStrategy.handleSynchronizationMessage(message.asInstanceOf[SynchronizationMessage], sender, this, this.getCurrentSimulationTime())
      }else {
        this.receiveMessage(message, sender)
      }
    }catch{
      case e: Throwable => throw new UnhandledChasquiException(this, message, sender, this.getCurrentSimulationTime(), e)
    }
  }
  def receiveMessage(message: Any, sender: Messaging)

  override def receive: Receive = {
    case "test" => sender ! "works"
    case SetId(id) => this.setId(id)
    case GetIngoingConnections => sender ! this.getIngoingConnections
    case GetIngoingConnections(role) => sender ! this.getIngoingConnections(role)
    case GetOutgoingConnections => sender ! this.getOutgoingConnections
    case GetMessageTransferDeltaInCurrentQuantum => sender ! this.getMessageDeltaInQuantum
    case ConnectTo(node, role) => this.connectTo(new NodeActorWrapper(node), role)
    case AddIngoingConnectionTo(actor, role) => this.addIngoingConnectionTo(new NodeActorWrapper(actor), role)
    case ReceiveMessage(message, sender) => this.receiveMessage(message, sender)
    case SetTime(time) => this.setTime(time)
    case GetCurrentSimulationTime => sender ! this.getCurrentSimulationTime()
    case SetSynchronizerStrategy(strategy) => this.setSynchronizerStrategy(strategy)
    case CheckPendingMessagesInQueue => this.checkPendingMessagesInQueue()
    case NotifyFinishedQuantum => this.notifyFinishedQuantum()
    case GetIncomingQuantum => sender ! this.getRealIncomingQuantum()
    case AdvanceSimulationTime => this.advanceSimulationTime()
    case AdvanceSimulationTime(nextQuantum) => this.advanceSimulationTime(nextQuantum)
    case SetCausalityErrorStrategy(strategy) => this.setCausalityErrorStrategy(strategy)
    case GetScheduledMessages => sender ! this.getScheduledMessages
    case HasPendingMessages => sender ! this.hasPendingMessages()
    case HasPendingMessagesOfTimestamp(timestamp) => this.hasPendingMessagesOfTimestamp(timestamp)
    case BroadcastMessageToIncoming(message, timestamp) => this.broadcastMessage(timestamp, message)
    case SendMessage(receiver, message, timestamp) => this.sendMessage(receiver, timestamp, message)
    case ScheduleMessage(message, timestamp, sender) => this.scheduleMessage(message, timestamp, sender)
  }
}

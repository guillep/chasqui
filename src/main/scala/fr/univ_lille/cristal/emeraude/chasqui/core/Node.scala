package fr.univ_lille.cristal.emeraude.chasqui.core

import akka.actor.TypedActor

import scala.collection.mutable

/**
  * Created by guille on 10/04/17.
  */
trait Node {
  def receiveMessage(message: Any, sender: Node): Unit
  def sendMessage(receiver: Node, timestamp: Int, message: Any): Any
  def scheduleMessage(message: Any, timestamp: Int, sender: Node): Unit

  def getOutgoingConnections: Set[Node]
  def getIngoingConnections: Set[Node]

  def connectTo(node: Node): Unit
  def addIngoingConnectionTo(node: Node): Unit

  def setTime(t: Int): Unit
  def getCurrentSimulationTime(): Int
  def setSynchronizerStrategy(synchronizerStrategy: SynchronizerStrategy): Unit
  def start(): Unit
  def advanceSimulationTime(): Unit
  def setCausalityErrorStrategy(causalityErrorStrategy: CausalityErrorStrategy): Unit

  def getScheduledMessages: mutable.PriorityQueue[Message]
}

abstract class NodeImpl(private var causalityErrorStrategy : CausalityErrorStrategy = new ErrorCausalityErrorStrategy) extends Node {

  private var synchronizerStrategy: SynchronizerStrategy = new ManualSynchronizerStrategy
  private var currentSimulationTime = 0
  private val messageQueue = scala.collection.mutable.PriorityQueue[Message]()(Ordering.fromLessThan((s1, s2) => s1.getTimestamp > s2.getTimestamp))

  private val outgoingConnections = new scala.collection.mutable.HashSet[Node]()
  private val ingoingConnections = new scala.collection.mutable.HashSet[Node]()

  def self: Node = TypedActor.self

  def connectTo(node: Node): Unit = {
    outgoingConnections.add(node)
    node.addIngoingConnectionTo(self)
  }

  def addIngoingConnectionTo(node: Node): Unit = ingoingConnections.add(node)

  def getOutgoingConnections: Set[Node] = outgoingConnections.toSet
  def getIngoingConnections: Set[Node] = ingoingConnections.toSet

  def setTime(t: Int): Unit = {
    this.currentSimulationTime = t
  }
  def getCurrentSimulationTime(): Int = this.currentSimulationTime

  def setSynchronizerStrategy(synchronizerStrategy: SynchronizerStrategy): Unit ={
    this.synchronizerStrategy = synchronizerStrategy
    this.synchronizerStrategy.registerNode(self)
  }

  def advanceSimulationTime(): Unit = {
    this.currentSimulationTime += 1
    this.start()
  }


  def start() = {
    //Get all elements in the same priority and remove them from the message queue
    val currentTimestampMessages = new mutable.Queue[Message]()
    while (this.messageQueue.nonEmpty &&
      this.messageQueue.head.getTimestamp == this.currentSimulationTime) {
      currentTimestampMessages.enqueue(this.messageQueue.dequeue())
    }

    currentTimestampMessages.foreach(m => this.receiveMessage(m.getMessage, m.getSender))
    this.synchronizerStrategy.notifyFinishedTime(self, this.currentSimulationTime, this.getScheduledMessages.size)
  }

  def queueMessage(message: Any, timestamp: Int, sender: Node): Unit = {
    messageQueue += new Message(message, timestamp, sender)
  }

  def getScheduledMessages: mutable.PriorityQueue[Message] = this.messageQueue


  def sendMessage(receiver: Node, timestamp: Int, message: Any): Any = receiver.scheduleMessage(message, timestamp, self)

  def scheduleMessage(message: Any, timestamp: Int, sender: Node): Unit = {
    if (timestamp < this.currentSimulationTime) {
      //The message is in the past.
      //This is a Causality error
      causalityErrorStrategy.handleCausalityError(timestamp, this.currentSimulationTime, self, sender, message)
      return
    }

    if (this.currentSimulationTime == timestamp){
      this.receiveMessage(message, sender)
    } else {
      this.queueMessage(message, timestamp, sender)
    }
  }

  def setCausalityErrorStrategy(causalityErrorStrategy: CausalityErrorStrategy): Unit = {
    this.causalityErrorStrategy = causalityErrorStrategy
  }

  def receiveMessage(message: Any, sender: Node)

}

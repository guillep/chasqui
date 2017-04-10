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
  def advanceSimulationTime(): Unit
  def setCausalityErrorStrategy(causalityErrorStrategy: CausalityErrorStrategy): Unit

  def getScheduledMessages: mutable.PriorityQueue[Message]
}

abstract class NodeImpl(private var causalityErrorStrategy : CausalityErrorStrategy = new ErrorCausalityErrorStrategy) {


  private var currentSimulationTime = 0
  private val messageQueue = scala.collection.mutable.PriorityQueue[Message]()(Ordering.fromLessThan((s1, s2) => s1.getTimestamp > s2.getTimestamp))

  private val outgoingConnections = new scala.collection.mutable.HashSet[Node]()
  private val ingoingConnections = new scala.collection.mutable.HashSet[Node]()

  def getThisNode: Node = TypedActor.self

  def connectTo(node: Node): Unit = {
    outgoingConnections.add(node)
    node.addIngoingConnectionTo(this.getThisNode)
  }

  def addIngoingConnectionTo(node: Node): Unit = ingoingConnections.add(node)

  def getOutgoingConnections: Set[Node] = outgoingConnections.toSet
  def getIngoingConnections: Set[Node] = ingoingConnections.toSet

  def setTime(t: Int): Unit = currentSimulationTime = t
  def advanceSimulationTime(): Unit = {
    this.currentSimulationTime += 1
    val currentTimestampMessages = this.messageQueue.takeWhile(m => m.getTimestamp == this.currentSimulationTime)
    currentTimestampMessages.foreach(m => this.receiveMessage(m.getMessage, m.getSender))
  }


  def queueMessage(message: Any, timestamp: Int, sender: Node): Unit = {
    messageQueue += new Message(message, timestamp, sender)
  }

  def getScheduledMessages: mutable.PriorityQueue[Message] = this.messageQueue


  def sendMessage(receiver: Node, timestamp: Int, message: Any): Any = receiver.scheduleMessage(message, timestamp, this.getThisNode)
  
  def scheduleMessage(message: Any, timestamp: Int, sender: Node): Unit = {
    if (timestamp < this.currentSimulationTime) {
      //The message is in the past.
      //This is a Causality error
      causalityErrorStrategy.handleCausalityError(timestamp, this.currentSimulationTime, this.getThisNode, sender, message)
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

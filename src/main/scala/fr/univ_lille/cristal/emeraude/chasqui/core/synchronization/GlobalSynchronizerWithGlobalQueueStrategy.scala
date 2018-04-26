package fr.univ_lille.cristal.emeraude.chasqui.core.synchronization

import akka.actor.{Actor, ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props}
import akka.util.Timeout
import fr.univ_lille.cristal.emeraude.chasqui.core.Node.ReceiveMessage
import fr.univ_lille.cristal.emeraude.chasqui.core.{Message, Node, SynchronizationMessage, SynchronizerStrategy}

import scala.concurrent.ExecutionContext

case class SynchronizeMessage(sender: ActorRef, receiver: ActorRef, timestamp: Long, message: Any)

class GlobalSynchronizerWithGlobalQueueStrategy(system: ActorSystem) extends SynchronizerStrategy {
  private var sentMessagesInQuantum = 0
  private var receivedMessagesInQuantum = 0

  private val messageQueue = scala.collection.mutable.PriorityQueue[Message]()(Ordering.fromLessThan((s1, s2) => s1.getTimestamp > s2.getTimestamp))

  def registerNode(node: Node): Unit = {

  }

  def notifyFinishedTime(nodeActorRef: ActorRef, node: Node, t: Long, messageDelta: Int): Unit = {

  }

  def getSynchronizerActor() = {
    GlobalSynchronizerWithGlobalQueueStrategyAccessor(system).instance
  }

  override def handleSynchronizationMessage(message: SynchronizationMessage, sender: ActorRef, receiver: Node, t: Long): Unit = {
    //Nothing
  }

  override def sendMessage(senderNode: Node, receiverActor: ActorRef, messageTimestamp: Long, message: Any): Unit = {
    this.getSynchronizerActor() ! SynchronizeMessage(senderNode.getActorRef, receiverActor, messageTimestamp, message)
  }

  override def scheduleMessage(receiverNode: Node, senderActor: ActorRef, messageTimestamp: Long, message: Any): Unit = {
    this.receivedMessagesInQuantum += 1

    if (messageTimestamp < receiverNode.getCurrentSimulationTime) {
      //The message is in the past.
      //This is a Causality error
      if (!message.isInstanceOf[SynchronizationMessage]) {
        receiverNode.getCausalityErrorStrategy.handleCausalityError(messageTimestamp, receiverNode.getCurrentSimulationTime, receiverNode, senderActor, message)
      }
      return
    }

    if (receiverNode.getCurrentSimulationTime == messageTimestamp) {
      receiverNode.handleIncomingMessage(message, senderActor)
    } else {
      this.queueMessage(senderActor, messageTimestamp, message)
    }
    receiverNode.notifyFinishedQuantum()
  }

  private def queueMessage(senderActor: ActorRef, messageTimestamp: Long, message: Any) = {
    messageQueue += new Message(message, messageTimestamp, senderActor)
  }

  override def getMessageQueue: scala.collection.mutable.PriorityQueue[Message] = this.messageQueue
}

/** **************************************************************************
  *
  * Actor Singleton Accessor
  *
  * ***************************************************************************/


class GlobalSynchronizerWithGlobalQueueSingletonActor extends Actor {

  import scala.concurrent.duration._

  implicit val ec = ExecutionContext.Implicits.global
  implicit lazy val timeout = Timeout(5 seconds)

  private var getCurrentTimestamp = 0
  private var remainingAcks = 0

  private val messageQueue = scala.collection.mutable.PriorityQueue[SynchronizeMessage]()(Ordering.fromLessThan((s1, s2) => s1.timestamp > s2.timestamp))

  val nodesFinishedThisQuantum = new collection.mutable.HashSet[ActorRef]()
  var messagesToBeProcessedFollowingQuantums: Int = 0

  def synchronizeMessage(aSynchronizeMessage: SynchronizeMessage): Unit = {
    if  (this.getCurrentTimestamp == aSynchronizeMessage.timestamp) {
      aSynchronizeMessage.receiver ! ReceiveMessage(aSynchronizeMessage.message, aSynchronizeMessage.sender)
      this.remainingAcks += 1
    } else {
      this.messageQueue += aSynchronizeMessage
    }
  }

  override def receive: Receive = {
    case synchronizeMessage: SynchronizeMessage => this.synchronizeMessage(synchronizeMessage)
  }
}


/** **************************************************************************
  *
  * Actor Singleton Accessor
  *
  * ***************************************************************************/

class GlobalSynchronizerWithGlobalQueueSingleton(system: ActorSystem, props: Props, name: String) extends Extension {
  val instance: ActorRef = system.actorOf(props, name)
}

object GlobalSynchronizerWithGlobalQueueStrategyAccessor extends ExtensionId[GlobalSynchronizerWithGlobalQueueSingleton] with ExtensionIdProvider {
  final override def lookup = this

  final override def createExtension(system: ExtendedActorSystem) = new GlobalSynchronizerWithGlobalQueueSingleton(system, instanceProps, instanceName)

  lazy val instanceProps = Props[GlobalSynchronizerWithGlobalQueueSingletonActor]()
  lazy val instanceName = "global-synchronizer-global-queue-actor"
}
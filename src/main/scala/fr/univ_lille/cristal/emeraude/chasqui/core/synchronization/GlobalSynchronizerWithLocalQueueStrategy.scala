package fr.univ_lille.cristal.emeraude.chasqui.core.synchronization

import akka.actor.{Actor, ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props}
import akka.pattern.ask
import akka.util.Timeout
import fr.univ_lille.cristal.emeraude.chasqui.core.Node._
import fr.univ_lille.cristal.emeraude.chasqui.core._
import fr.univ_lille.cristal.emeraude.chasqui.core.synchronization.GlobalSynchronizerWithLocalQueueStrategy.{NotifyFinishedTime, RegisterNode}

import scala.collection.mutable
import scala.concurrent.{ExecutionContext, Future}
import scala.util._

abstract class SynchronizerStrategyCompanion {
  def buildFor(system: ActorSystem): SynchronizerStrategy
}

object GlobalSynchronizerWithLocalQueueStrategy extends SynchronizerStrategyCompanion {
  override def buildFor(system: ActorSystem): SynchronizerStrategy = new GlobalSynchronizerWithLocalQueueStrategy(system)

  case class RegisterNode(node: ActorRef)
  case class NotifyFinishedTime(node: ActorRef, finishedQuantum: Long, messageQueueSize: Int, messageDelta: Int, incomingQuantum: Option[Long])
}

class GlobalSynchronizerWithLocalQueueStrategy(system: ActorSystem) extends SynchronizerStrategy {
  private var sentMessagesInQuantum = 0
  private var receivedMessagesInQuantum = 0

  private val messageQueue = scala.collection.mutable.PriorityQueue[Message]()(Ordering.fromLessThan((s1, s2) => s1.getTimestamp > s2.getTimestamp))

  def registerNode(node: Node): Unit = {
    this.getSynchronizerActor() ! RegisterNode(node.getActorRef)
  }

  def notifyFinishedTime(nodeActorRef: ActorRef, node: Node, t: Long, messageDelta: Int): Unit = {
    this.getSynchronizerActor() ! NotifyFinishedTime(nodeActorRef, t, this.messageQueue.size, messageDelta, node.getRealIncomingQuantum)
  }

  def getSynchronizerActor(): ActorRef = {
    GlobalSynchronizerWithLocalQueueStrategyAccessor(system).instance
  }

  override def handleSynchronizationMessage(message: SynchronizationMessage, sender: ActorRef, receiver: Node, t: Long): Unit = {
    //Nothing
  }

  override def sendMessage(senderNode: Node, receiverActor: ActorRef, messageTimestamp: Long, message: Any): Unit = {
    this.sentMessagesInQuantum += 1
    receiverActor ! ScheduleMessage(message, messageTimestamp, senderNode.getActorRef)
  }

  override def scheduleMessage(receiverNode: Node, senderActor: ActorRef, messageTimestamp: Long, message: Any): Unit = {
    this.receivedMessagesInQuantum += 1

    if (messageTimestamp < receiverNode.getCurrentSimulationTime) {
      //The message is in the past.
      //This is a Causality error unless it is a SynchronizationMessages
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

  override def getMessageQueue: scala.collection.mutable.PriorityQueue[Message] = this.messageQueue
}

abstract class AbstractGlobalSynchronizerWithLocalQueueSingletonActor extends Actor {
  val nodes = new collection.mutable.HashSet[ActorRef]()

  def registerNode(node: ActorRef): Unit = {
    nodes += node
  }

  def notifyFinishedTime(nodeActorRef: ActorRef, t: Long, queueSize: Int, messageDelta: Int, incomingQuantum: Option[Long]): Unit

  override def receive: Receive = {
    case RegisterNode(node) => this.registerNode(node)
    case NotifyFinishedTime(node: ActorRef, finishedQuantum: Long, messageQueueSize: Int, messageDelta: Int, incomingQuantum: Option[Long]) =>
        this.notifyFinishedTime(node, finishedQuantum, messageQueueSize, messageDelta, incomingQuantum)
  }
}

class GlobalSynchronizerWithLocalQueueSingletonActor extends AbstractGlobalSynchronizerWithLocalQueueSingletonActor {


  import scala.concurrent.duration._
  implicit val ec = ExecutionContext.Implicits.global
  implicit lazy val timeout = Timeout(5 seconds)

  val nodeMessageDeltas = new mutable.HashMap[ActorRef, Int]()
  val nodeIncomingQuantums = new mutable.HashMap[ActorRef, Option[Long]]()
  val nodesFinishedThisQuantum = new collection.mutable.HashSet[ActorRef]()
  var messagesToBeProcessedFollowingQuantums: Int = 0

  protected def allNodesAreReady(): Boolean = {
    nodes.forall(this.nodesFinishedThisQuantum.contains)
  }

  def getNextQuantum(): Option[Long] = {
    nodeIncomingQuantums.values.foldLeft[Option[Long]](None)( (accum, each) =>
      if (accum.isEmpty) {
        each
      } else if (each.isEmpty) {
        accum
      } else {
        Some(accum.get.min(each.get))
      }
    )
  }

  def numberOfPendingMessagesInQuantum(): Int = nodeMessageDeltas.values.sum

  def setMessageDelta(nodeActorRef: ActorRef, messageDelta: Int) = {
    nodeMessageDeltas(nodeActorRef) = messageDelta
  }

  def setIncomingQuantum(nodeActorRef: ActorRef, incomingQuantum: Option[Long]) = {
    nodeIncomingQuantums(nodeActorRef) = incomingQuantum
  }

  def notifyFinishedTime(nodeActorRef: ActorRef, t: Long, queueSize: Int, messageDelta: Int, incomingQuantum: Option[Long]): Unit = {

    this.nodesFinishedThisQuantum += nodeActorRef
    this.setMessageDelta(nodeActorRef, messageDelta)
    this.setIncomingQuantum(nodeActorRef, incomingQuantum)
    this.messagesToBeProcessedFollowingQuantums += queueSize

    val allNodesReady = this.allNodesAreReady()
    val numberOfPendingMessagesInQuantum = this.numberOfPendingMessagesInQuantum()
    val existPendingMessages = this.messagesToBeProcessedFollowingQuantums != 0
    if (allNodesReady && numberOfPendingMessagesInQuantum == 0 && existPendingMessages) {
      val maybeNextQuantum = this.getNextQuantum()
      if (maybeNextQuantum.isDefined) {
        val sequence = this.nodes.map(node => (node ? AdvanceSimulationTime(maybeNextQuantum.get)).asInstanceOf[Future[Int]])
        Future.sequence(sequence)
          .onComplete {
            case Success(result) => {
              //TODO: println(s"Quantum achieved: ${maybeNextQuantum.get} with: $result")
              this.nodes.foreach(n => n ! ProcessNextQuantum)
            }
            case Failure(_) => {
              println("Error while resuming next quantum!")
            }
          }

        //Cleanup local state
        this.nodesFinishedThisQuantum.clear()
        this.nodeIncomingQuantums.clear()
        this.nodeMessageDeltas.clear()
        this.messagesToBeProcessedFollowingQuantums = 0
      } else {
        //Finished?
      }
    } else {
      //println(s"Not ready to advance yet at t=$t. Nodes ready: $allNodesReady, all messages in quantum processed: $numberOfPendingMessagesInQuantum, existing pending messages: $existPendingMessages")
    }
  }
}


class GlobalSynchronizerWithLocalQueueSingleton(system: ActorSystem, props: Props, name: String) extends Extension {
  val instance: ActorRef = system.actorOf(props, name)
}

object GlobalSynchronizerWithLocalQueueStrategyAccessor extends ExtensionId[GlobalSynchronizerWithLocalQueueSingleton] with ExtensionIdProvider {
  final override def lookup = this
  final override def createExtension(system: ExtendedActorSystem) = new GlobalSynchronizerWithLocalQueueSingleton(system, instanceProps, instanceName)

  lazy val instanceProps = Props[GlobalSynchronizerWithLocalQueueSingletonActor]
  lazy val instanceName = "global-synchronizer-actor"
}
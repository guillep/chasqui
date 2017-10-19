package fr.univ_lille.cristal.emeraude.chasqui.core.synchronization

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, TypedActor, TypedProps}
import akka.pattern.ask
import akka.util.Timeout
import fr.univ_lille.cristal.emeraude.chasqui.core.Node._
import fr.univ_lille.cristal.emeraude.chasqui.core._

import scala.concurrent.{Await, ExecutionContext, Future}
import scala.util._

/**
  * Created by guille on 19/04/17.
  */
class GlobalSynchronizerWithLocalQueueStrategy(system: ActorSystem) extends SynchronizerStrategy {
  private var sentMessagesInQuantum = 0
  private var receivedMessagesInQuantum = 0

  private val messageQueue = scala.collection.mutable.PriorityQueue[Message]()(Ordering.fromLessThan((s1, s2) => s1.getTimestamp > s2.getTimestamp))

  def registerNode(node: Node): Unit = {
    this.getSynchronizerActor().registerNode(node.getActorRef)
  }

  def notifyFinishedTime(nodeActorRef: ActorRef, node: Node, t: Long, messageDelta: Int): Unit = {
    this.getSynchronizerActor().notifyFinishedTime(nodeActorRef, t, this.messageQueue.size, messageDelta)
  }

  def getSynchronizerActor() = {
    GlobalSynchronizerWithLocalQueueStrategyAccessor(system).instance
  }

  override def handleSynchronizationMessage(message: SynchronizationMessage, sender: ActorRef, receiver: Node, t: Long): Unit = {
    //Nothing
  }

  override def sendMessage(senderNode: NodeImpl, receiverActor: ActorRef, messageTimestamp: Long, message: Any): Unit = {
    this.sentMessagesInQuantum += 1
    receiverActor ! ScheduleMessage(message, messageTimestamp, senderNode.getActorRef)
  }

  override def scheduleMessage(receiverNode: NodeImpl, senderActor: ActorRef, messageTimestamp: Long, message: Any): Unit = {
    this.receivedMessagesInQuantum += 1

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

  override def getMessageQueue: scala.collection.mutable.PriorityQueue[Message] = this.messageQueue
}

trait MessageSynchronizer {
  val nodes = new collection.mutable.HashSet[ActorRef]()

  def registerNode(node: ActorRef): Unit = {
    nodes += node
  }

  def notifyFinishedTime(nodeActorRef: ActorRef, t: Long, queueSize: Int, messageDelta: Int): Unit

}

class GlobalSynchronizerSingletonActor extends MessageSynchronizer {

  import scala.concurrent.duration._
  implicit val ec = ExecutionContext.Implicits.global
  implicit lazy val timeout = Timeout(5 seconds)

  val nodesFinishedThisQuantum = new collection.mutable.HashSet[ActorRef]()
  var messagesToBeProcessedFollowingQuantums: Int = 0

  protected def allNodesAreReady(): Boolean = {
    nodes.forall(this.nodesFinishedThisQuantum.contains)
  }

  def getNextQuantum(): Option[Long] = {

    val sequence = nodes.toList.map(node => (node ? GetIncomingQuantum).asInstanceOf[Future[Option[Long]]])
    val total = Future.foldLeft[Option[Long], Option[Long]](sequence)(None)((accum, each)=>
      if (accum.isEmpty) {
        each
      } else if (each.isEmpty) {
        accum
      } else {
        Some(accum.get.min(each.get))
      })
    Await.result(total, Timeout(5, TimeUnit.MINUTES).duration)
  }

  def allMessagesInThisQuantumProcessed(): Int = {
    val sequence = nodes.toList.map(node => (node ? GetMessageTransferDeltaInCurrentQuantum).asInstanceOf[Future[Int]])
    val total = Future.foldLeft[Int, Int](sequence)(0)((accum, each)=> accum + each)
    Await.result(total, Timeout(5, TimeUnit.MINUTES).duration)
  }

  def notifyFinishedTime(nodeActorRef: ActorRef, t: Long, queueSize: Int, messageDelta: Int): Unit = {

    this.nodesFinishedThisQuantum += nodeActorRef
    this.messagesToBeProcessedFollowingQuantums += queueSize

    val allNodesReady = this.allNodesAreReady()
    val allMessagesInThisQuantumProcessed = this.allMessagesInThisQuantumProcessed()
    val existPendingMessages = this.messagesToBeProcessedFollowingQuantums != 0
    if (allNodesReady && allMessagesInThisQuantumProcessed == 0 && existPendingMessages) {
      this.nodesFinishedThisQuantum.clear()
      this.messagesToBeProcessedFollowingQuantums = 0

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
      }
    } else {
      //println(s"Not ready to advance yet at t=$t. Nodes ready: $allNodesReady, all messages in quantum processed: $allMessagesInThisQuantumProcessed, existing pending messages: $existPendingMessages")
    }
  }
}


class GlobalSynchronizerSingleton[T <: AnyRef, TImpl <: T](system: ActorSystem, props: TypedProps[TImpl], name: String) extends Extension {
  val instance: T = TypedActor(system).typedActorOf[T, TImpl](props, name)
}

object GlobalSynchronizerWithLocalQueueStrategyAccessor extends ExtensionId[GlobalSynchronizerSingleton[MessageSynchronizer, GlobalSynchronizerSingletonActor]] with ExtensionIdProvider {
  final override def lookup = this
  final override def createExtension(system: ExtendedActorSystem) = new GlobalSynchronizerSingleton(system, instanceProps, instanceName)

  lazy val instanceProps = TypedProps[GlobalSynchronizerSingletonActor]()
  lazy val instanceName = "global-synchronizer-actor"
}
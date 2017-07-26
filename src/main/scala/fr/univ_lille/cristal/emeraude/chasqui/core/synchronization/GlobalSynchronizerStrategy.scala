package fr.univ_lille.cristal.emeraude.chasqui.core.synchronization

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, TypedActor, TypedProps}
import akka.pattern.ask
import akka.util.Timeout
import fr.univ_lille.cristal.emeraude.chasqui.core.Node.{AdvanceSimulationTime, GetMessageTransferDeltaInCurrentQuantum}
import fr.univ_lille.cristal.emeraude.chasqui.core._

import scala.concurrent.{Await, ExecutionContext, Future}

/**
  * Created by guille on 19/04/17.
  */
class GlobalSynchronizerStrategy(system: ActorSystem) extends SynchronizerStrategy {


  def registerNode(node: Node): Unit = {
    this.getSynchronizerActor().registerNode(node.getActorRef())
  }

  def notifyFinishedTime(nodeActorRef: ActorRef, node: Node, t: Long, queueSize: Int, messageDelta: Int): Unit = {
    this.getSynchronizerActor().notifyFinishedTime(nodeActorRef, t, queueSize, messageDelta)
  }

  def getSynchronizerActor() = {
    SingletonService(system).instance
  }

  override def handleSynchronizationMessage(message: SynchronizationMessage, sender: ActorRef, receiver: Node, t: Long): Unit = {
    //Nothing
  }
}

trait MessageSynchronizer {
  val nodes = new collection.mutable.HashSet[ActorRef]()

  def registerNode(node: ActorRef): Unit = {
    nodes += node
  }

  def notifyFinishedTime(nodeActorRef: ActorRef, t: Long, queueSize: Int, messageDelta: Int): Unit

}

class MessageSynchronizerImpl extends MessageSynchronizer {
  import scala.concurrent.duration._
  implicit val ec = ExecutionContext.Implicits.global
  implicit lazy val timeout = Timeout(5 seconds)

  val nodesFinishedThisQuantum = new collection.mutable.HashSet[ActorRef]()
  var messagesToBeProcessedFollowingQuantums: Int = 0

  protected def allNodesAreReady(): Boolean = {
    nodes.forall(this.nodesFinishedThisQuantum.contains)
  }

  def allMessagesInThisQuantumProcessed(): Boolean = {
    val sequence = nodes.toList.map(node => (node ? GetMessageTransferDeltaInCurrentQuantum).asInstanceOf[Future[Int]])
    val total = Future.foldLeft[Int, Int](sequence)(0)((accum, each)=> accum + each)
    Await.result(total, Timeout(5, TimeUnit.MINUTES).duration) == 0
  }

  def notifyFinishedTime(nodeActorRef: ActorRef, t: Long, queueSize: Int, messageDelta: Int): Unit = {

    this.nodesFinishedThisQuantum += nodeActorRef
    this.messagesToBeProcessedFollowingQuantums += queueSize

    val allNodesReady = this.allNodesAreReady()
    val allMessagesInThisQuantumProcessed = this.allMessagesInThisQuantumProcessed()
    if (allNodesReady && allMessagesInThisQuantumProcessed && (this.messagesToBeProcessedFollowingQuantums != 0)) {
      this.nodesFinishedThisQuantum.clear()
      this.messagesToBeProcessedFollowingQuantums = 0
      this.nodes.foreach(node => node ! AdvanceSimulationTime)
    }
  }
}


class GlobalSynchronizerSingleton[T <: AnyRef, TImpl <: T](system: ActorSystem, props: TypedProps[TImpl], name: String) extends Extension {
  val instance: T = TypedActor(system).typedActorOf[T, TImpl](props, name)
}

trait SystemScoped extends ExtensionId[GlobalSynchronizerSingleton[MessageSynchronizer, MessageSynchronizerImpl]] with ExtensionIdProvider {
  final override def lookup = this
  final override def createExtension(system: ExtendedActorSystem) = new GlobalSynchronizerSingleton(system, instanceProps, instanceName)

  protected def instanceProps: TypedProps[MessageSynchronizerImpl]
  protected def instanceName: String
}

object SingletonService extends SystemScoped {
  override lazy val instanceProps = TypedProps[MessageSynchronizerImpl]()
  override lazy val instanceName = "global-synchronizer-actor"
}
package fr.univ_lille.cristal.emeraude.chasqui.core

import java.util.concurrent.TimeUnit

import akka.actor.{ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, TypedActor, TypedProps}
import akka.util.Timeout

import scala.concurrent.{Await, Future}

/**
  * Created by guille on 19/04/17.
  */
class GlobalSynchronizerStrategy(system: ActorSystem) extends SynchronizerStrategy{


  def registerNode(node: Node): Unit = {
    this.getSynchronizerActor().registerNode(node)
  }

  def notifyFinishedTime(node: Node, t: Long, queueSize: Int, messageDelta: Int): Unit = {
    this.getSynchronizerActor().notifyFinishedTime(node, t, queueSize, messageDelta)
  }

  def getSynchronizerActor() = {
    SingletonService(system).instance
  }

  override def handleSynchronizationMessage(message: SynchronizationMessage, sender: Messaging, receiver: Node): Unit = {
    //Nothing
  }
}

trait MessageSynchronizer {
  val nodes = new collection.mutable.HashSet[Node]()

  def registerNode(node: Node): Unit = {
    nodes += node
  }

  def notifyFinishedTime(node: Node, t: Long, queueSize: Int, messageDelta: Int): Unit

}

class MessageSynchronizerImpl extends MessageSynchronizer {

  import TypedActor.dispatcher

  val nodesFinishedThisQuantum = new collection.mutable.HashSet[Node]()
  var messagesToBeProcessedFollowingQuantums: Int = 0

  protected def allNodesAreReady(): Boolean = {
    nodes.forall(this.nodesFinishedThisQuantum.contains(_))
  }

  def allMessagesInThisQuantumProcessed(): Boolean = {
    val sequence = nodes.toList.map(node => node.getMessageTransferDeltaInCurrentQuantum())
    val total = Future.foldLeft[Int, Int](sequence)(0)((accum, each)=> accum + each)
    Await.result(total, Timeout(5, TimeUnit.MINUTES).duration) == 0
  }

  def notifyFinishedTime(node: Node, t: Long, queueSize: Int, messageDelta: Int): Unit = {

    this.nodesFinishedThisQuantum += node
    this.messagesToBeProcessedFollowingQuantums += queueSize

    val allNodesReady = this.allNodesAreReady() && this.allMessagesInThisQuantumProcessed()
    if (allNodesReady && (this.messagesToBeProcessedFollowingQuantums != 0)) {
      this.nodesFinishedThisQuantum.clear()
      this.messagesToBeProcessedFollowingQuantums = 0
      this.nodes.foreach(node => node.advanceSimulationTime() )
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
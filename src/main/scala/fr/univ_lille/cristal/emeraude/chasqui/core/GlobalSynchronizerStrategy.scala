package fr.univ_lille.cristal.emeraude.chasqui.core

import akka.actor.{Actor, ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, Props, TypedActor, TypedProps}

/**
  * Created by guille on 19/04/17.
  */
class GlobalSynchronizerStrategy(system: ActorSystem) extends SynchronizerStrategy{

  def registerNode(node: Node): Unit = {
    SingletonService(system).instance.registerNode(node)
  }

  def notifyFinishedTime(node: Node, t: Int, queueSize: Int): Unit = {
    SingletonService(system).instance.notifyFinishedTime(node, t, queueSize)
  }
}

trait MessageSynchronizer {
  val nodes = new collection.mutable.HashSet[Node]()

  def registerNode(node: Node): Unit = {
    nodes += node
  }

  def notifyFinishedTime(node: Node, t: Int, queueSize: Int): Unit

}

class MessageSynchronizerImpl extends MessageSynchronizer {

  val nodesFinishedThisQuantum = new collection.mutable.HashSet[Node]()
  var messagesToBeProcessedThisQuantum: Int = 0

  protected def allNodesAreReady(): Boolean = {
    this.nodes.forall( node => this.nodesFinishedThisQuantum.contains(node) )
  }

  def notifyFinishedTime(node: Node, t: Int, queueSize: Int): Unit = {

    this.nodesFinishedThisQuantum += node
    this.messagesToBeProcessedThisQuantum += queueSize

    if (this.allNodesAreReady() && this.messagesToBeProcessedThisQuantum != 0) {
      this.nodesFinishedThisQuantum.clear()
      this.messagesToBeProcessedThisQuantum = 0

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
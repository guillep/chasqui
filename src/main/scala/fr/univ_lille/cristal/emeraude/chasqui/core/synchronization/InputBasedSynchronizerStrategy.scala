package fr.univ_lille.cristal.emeraude.chasqui.core.synchronization

import java.util.concurrent.TimeUnit

import akka.actor.{ActorRef, ActorSystem, ExtendedActorSystem, Extension, ExtensionId, ExtensionIdProvider, TypedActor, TypedProps}
import akka.util.Timeout
import fr.univ_lille.cristal.emeraude.chasqui.core._
import fr.univ_lille.cristal.emeraude.chasqui.porting.InputActor.PushSpikes

import scala.concurrent.{Await, Future}

/**
  * Created by guille on 19/04/17.
  */
class InputBasedSynchronizerStrategy(system: ActorSystem) extends SynchronizerStrategy{


  def registerNode(node: Node): Unit = {
    this.getSynchronizerActor().registerNode(node)
  }

  def notifyFinishedTime(nodeActor: Node, node: Node, t: Long, queueSize: Int, messageDelta: Int): Unit = {
    this.getSynchronizerActor().notifyFinishedTime(nodeActor, t, queueSize, messageDelta)
  }

  def getSynchronizerActor() = {
    InputBasedSingletonService(system).instance
  }

  override def handleSynchronizationMessage(message: SynchronizationMessage, sender: Messaging, receiver: Node, t: Long): Unit = {
    //Nothing
  }
}

trait InputMessageSynchronizer extends MessageSynchronizer {
  def setInputActor(actor: ActorRef)
}

class InputBasedMessageSynchronizerImpl extends InputMessageSynchronizer {

  import TypedActor.dispatcher

  val nodesFinishedThisQuantum = new collection.mutable.HashSet[Node]()
  var messagesToBeProcessedFollowingQuantums: Int = 0
  var inputActor: Option[ActorRef] = None

  def setInputActor(actor: ActorRef) = {
    inputActor = Some(actor)
  }

  protected def allNodesAreReady(): Boolean = {
    nodes.forall(this.nodesFinishedThisQuantum.contains(_))
  }

  def allMessagesInThisQuantumProcessed(): Boolean = {
    val sequence = nodes.toList.map(node => node.getMessageTransferDeltaInCurrentQuantum())
    val total = Future.foldLeft[Int, Int](sequence)(0)((accum, each)=> accum + each)
    Await.result(total, Timeout(5, TimeUnit.MINUTES).duration) == 0
  }

  def askForNextQuantum(): Long = {
    val total = Future
      .sequence(nodes.toList.map(node => node.getIncomingQuantum()))
      .map(_.filter(_.isDefined).map(_.get).reduce((a,b) => math.min(a,b)))
    Await.result(total, Timeout(5, TimeUnit.MINUTES).duration)
  }

  def notifyFinishedTime(node: Node, t: Long, queueSize: Int, messageDelta: Int): Unit = {

    this.nodesFinishedThisQuantum += node
    this.messagesToBeProcessedFollowingQuantums += queueSize

    val allNodesReady = this.allNodesAreReady()
    val allMessagesInThisQuantumProcessed = this.allMessagesInThisQuantumProcessed()
    if (allNodesReady && (this.messagesToBeProcessedFollowingQuantums != 0)) {
      val nextQuantum = this.askForNextQuantum()
      this.nodesFinishedThisQuantum.clear()
      this.messagesToBeProcessedFollowingQuantums = 0
      this.nodes.foreach(node => node.advanceSimulationTime(nextQuantum))

      if(inputActor.isDefined){
        inputActor.get ! PushSpikes
      }
    }
  }
}


class InputBasedMessageSynchronizerSingleton[T <: AnyRef, TImpl <: T](system: ActorSystem, props: TypedProps[TImpl], name: String) extends Extension {
  val instance: T = TypedActor(system).typedActorOf[T, TImpl](props, name)
}

trait InputBasedSystemScoped extends ExtensionId[InputBasedMessageSynchronizerSingleton[InputMessageSynchronizer, InputBasedMessageSynchronizerImpl]] with ExtensionIdProvider {
  final override def lookup = this
  final override def createExtension(system: ExtendedActorSystem) = new InputBasedMessageSynchronizerSingleton(system, instanceProps, instanceName)

  protected def instanceProps: TypedProps[InputBasedMessageSynchronizerImpl]
  protected def instanceName: String
}

object InputBasedSingletonService extends InputBasedSystemScoped {
  override lazy val instanceProps = TypedProps[InputBasedMessageSynchronizerImpl]()
  override lazy val instanceName = "input-based-synchronizer-actor"
}
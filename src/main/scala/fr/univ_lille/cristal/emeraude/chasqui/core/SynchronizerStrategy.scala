package fr.univ_lille.cristal.emeraude.chasqui.core

import akka.actor.ActorRef

/**
  * Created by guille on 19/04/17.
  */
class SynchronizationMessage

trait SynchronizerStrategy {
  def registerNode(node: Node): Unit
  def notifyFinishedTime(nodeActorRef: ActorRef, node: Node, t: Long, messageDelta: Int): Unit
  def handleSynchronizationMessage(message: SynchronizationMessage, sender: ActorRef, receiver: Node, t: Long): Unit

  def sendMessage(senderNode: Node, receiverActor: ActorRef, messageTimestamp: Long, message: Any): Unit
  def scheduleMessage(receiverNode: Node, senderActor: ActorRef, messageTimestamp: Long, message: Any): Unit

  def getMessageQueue: scala.collection.mutable.PriorityQueue[Message]
}

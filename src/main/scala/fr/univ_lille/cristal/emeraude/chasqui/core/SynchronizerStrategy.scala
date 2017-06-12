package fr.univ_lille.cristal.emeraude.chasqui.core

import akka.actor.ActorRef

/**
  * Created by guille on 19/04/17.
  */
class SynchronizationMessage

trait SynchronizerStrategy {
  def registerNode(nodeActorRef: ActorRef): Unit
  def notifyFinishedTime(nodeActorRef: ActorRef, node: Node, t: Long, queueSize: Int, messageDelta: Int): Unit
  def handleSynchronizationMessage(message: SynchronizationMessage, sender: ActorRef, receiver: Node, t: Long): Unit
}

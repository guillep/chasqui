package fr.univ_lille.cristal.emeraude.chasqui.core

/**
  * Created by guille on 19/04/17.
  */
class SynchronizationMessage

trait SynchronizerStrategy {
  def registerNode(self: Node): Unit
  def notifyFinishedTime(node: Node, t: Long, queueSize: Int, messageDelta: Int): Unit
  def handleSynchronizationMessage(message: SynchronizationMessage, sender: Messaging, receiver: Node): Unit
}

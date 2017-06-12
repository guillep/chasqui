package fr.univ_lille.cristal.emeraude.chasqui.core.synchronization

import akka.actor.ActorRef
import fr.univ_lille.cristal.emeraude.chasqui.core.{Messaging, Node, SynchronizationMessage, SynchronizerStrategy}

/**
  * Created by guille on 19/04/17.
  */
class ManualSynchronizerStrategy extends SynchronizerStrategy {
  override def registerNode(self: ActorRef): Unit = {
    //Do nothing
    //We do not track any node
  }

  def notifyFinishedTime(nodeActor: ActorRef, node: Node, t: Long, queueSize: Int, messageDelta: Int): Unit = {
    //Do nothing
    //I leave the user with the entire responsibility of synchronizing
  }

  override def handleSynchronizationMessage(message: SynchronizationMessage, sender: Messaging, receiver: Node, t: Long): Unit = {
    //Nothing
  }
}

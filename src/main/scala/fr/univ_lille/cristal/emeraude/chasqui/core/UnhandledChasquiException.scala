package fr.univ_lille.cristal.emeraude.chasqui.core

import akka.actor.ActorRef

/**
  * Created by guille on 22/05/17.
  */
class UnhandledChasquiException(node: Node, message: Any, sender: ActorRef, timestamp: Long, originalException: Throwable) extends RuntimeException {

  override def toString: String = {
    s"Unhandled Chasqui Exception $originalException while node ${node} received message ${message} from node ${sender} at T=${timestamp}"
  }
}

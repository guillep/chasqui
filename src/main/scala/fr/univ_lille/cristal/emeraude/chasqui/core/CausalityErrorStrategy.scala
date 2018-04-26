package fr.univ_lille.cristal.emeraude.chasqui.core

import akka.actor.ActorRef

/**
  * Created by guille on 10/04/17.
  */
trait CausalityErrorStrategy {
  def handleCausalityError(causalityErrorTimestamp: Long, currentSimulationTime: Long, receiver: Node, sender: ActorRef, message: Any)
}

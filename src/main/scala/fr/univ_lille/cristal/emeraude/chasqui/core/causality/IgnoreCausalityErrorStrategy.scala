package fr.univ_lille.cristal.emeraude.chasqui.core.causality

import akka.actor.ActorRef
import fr.univ_lille.cristal.emeraude.chasqui.core.{CausalityErrorStrategy, Node}

/**
  * Created by guille on 10/04/17.
  */
class IgnoreCausalityErrorStrategy extends CausalityErrorStrategy {
  override def handleCausalityError(causalityErrorTimestamp: Long, currentSimulationTime: Long, receiver: Node, sender: ActorRef, message: Any): Unit = {
    //Do nothing, just ignore
  }
}

package fr.univ_lille.cristal.emeraude.chasqui.core.causality

import fr.univ_lille.cristal.emeraude.chasqui.core.{CausalityErrorStrategy, Messaging}

/**
  * Created by guille on 10/04/17.
  */
class IgnoreCausalityErrorStrategy extends CausalityErrorStrategy {
  override def handleCausalityError(causalityErrorTimestamp: Long, currentSimulationTime: Long, receiver: Messaging, sender: Messaging, message: Any): Unit = {
    //Do nothing, just ignore
  }
}

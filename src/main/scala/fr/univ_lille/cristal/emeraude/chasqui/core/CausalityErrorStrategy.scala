package fr.univ_lille.cristal.emeraude.chasqui.core

/**
  * Created by guille on 10/04/17.
  */
trait CausalityErrorStrategy {
  def handleCausalityError(causalityErrorTimestamp: Long, currentSimulationTime: Long, receiver: Messaging, sender: Messaging, message: Any)
}

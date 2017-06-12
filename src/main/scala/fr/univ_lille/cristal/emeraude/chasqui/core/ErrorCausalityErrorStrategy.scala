package fr.univ_lille.cristal.emeraude.chasqui.core

/**
  * Created by guille on 10/04/17.
  */
class ErrorCausalityErrorStrategy extends CausalityErrorStrategy {
  override def handleCausalityError(causalityErrorTimestamp: Long, currentSimulationTime: Long, receiver: Messaging, sender: Messaging, message: Any): Unit = {
    throw new CausalityErrorException(causalityErrorTimestamp, currentSimulationTime, receiver, sender, message)
  }
}

class CausalityErrorException(causalityErrorTimestamp: Long, currentSimulationTime: Long, receiver: Messaging, sender: Messaging, message: Any) extends RuntimeException {

  override def toString: String = {
    s"Causality Error: message $message from $sender to $receiver at t=$causalityErrorTimestamp while node $receiver was in t=$currentSimulationTime"
  }
}

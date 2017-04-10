package fr.univ_lille.cristal.emeraude.chasqui.core

/**
  * Created by guille on 10/04/17.
  */
class ErrorCausalityErrorStrategy extends CausalityErrorStrategy {
  override def handleCausalityError(causalityErrorTimestamp: Int, currentSimulationTime: Int, receiver: Node, sender: Node, message: Any): Unit = {
    throw new CausalityErrorException(causalityErrorTimestamp, currentSimulationTime, receiver, sender, message)
  }
}

class CausalityErrorException(causalityErrorTimestamp: Int, currentSimulationTime: Int, receiver: Node, sender: Node, message: Any) extends RuntimeException {

}

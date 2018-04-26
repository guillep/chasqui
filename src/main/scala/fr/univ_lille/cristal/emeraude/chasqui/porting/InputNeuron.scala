package fr.univ_lille.cristal.emeraude.chasqui.porting

import akka.actor.ActorRef
import fr.univ_lille.cristal.emeraude.chasqui.core._
import fr.univ_lille.cristal.emeraude.chasqui.porting.InputNeuron.SpikeAt

/**
  * Created by guille on 24/04/17.
  */
trait InputNeuron {
  def spikeAt(t: Long)
}

object InputNeuron {
  case class SpikeAt(t: Long)
}

class TypedInputNeuron(neuronRef: ActorRef) extends TypedNeuron(neuronRef) with InputNeuron {
  override def spikeAt(t: Long): Unit = {
    this.scheduleMessage(SpikeAt(t), t)
  }
}

class InputNeuronImpl extends NodeImpl with InputNeuron {

  override def scheduleMessage(message: Any, timestamp: Long, senderActorRef: ActorRef): Unit = {
    super.scheduleMessage(message, timestamp, senderActorRef)
  }

  override def receiveMessage(message: Any, sender: ActorRef): Unit = {
    message match {
      case SpikeAt(t) => this.spikeAt(t)
    }
  }

  override def spikeAt(t: Long): Unit = {
    //TODO: println(s"[$this] Spiking at $t. Local time = ${this.getCurrentSimulationTime()}")
    this.broadcastMessage(t, LIFNeuron.spikeMessage)
  }
}
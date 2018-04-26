package fr.univ_lille.cristal.emeraude.chasqui.porting

import akka.actor.ActorRef
import akka.pattern.ask
import fr.univ_lille.cristal.emeraude.chasqui.core.typed.TypedNode
import fr.univ_lille.cristal.emeraude.chasqui.core.{Node, NodeImpl}
import fr.univ_lille.cristal.emeraude.chasqui.porting.LIFNeuron.{GetIngoingConnectionWeights, SetMembraneThresholdPotential}

import scala.collection.mutable
import scala.concurrent.Future
import scala.math.exp
import scala.util.Random

/**
  * Created by guille on 19/04/17.
  */

object LIFNeuron {
  val inhibitMessage = "Inhibit"
  val spikeMessage = "Spike"
  val inhibitRoleName = "inhibitRole"

  object GetIngoingConnectionWeights
  case class SetMembraneThresholdPotential(potential: Int)
}

trait LIFNeuron extends Node with Synapse {
}

class TypedNeuron(neuronRef: ActorRef) extends TypedNode(neuronRef) {
  def setMembraneThresholdPotential(potential: Int): Unit = {
    actor ! SetMembraneThresholdPotential(potential)
  }

  def getIngoingConnectionWeights(): Future[Seq[Double]] = {
    (actor ? GetIngoingConnectionWeights).asInstanceOf[Future[Seq[Double]]]
  }
}

trait Synapse {
  protected def backfire()
  protected def addIngoingSynapse(node: ActorRef)
  protected def setLastSpikeOf(sender: ActorRef, t: Long)
  protected def getWeightOf(sender: ActorRef): Double
  protected def getIngoingConnectionWeights() : Seq[Double]
}

trait QBGUnsignedSynapse extends Synapse with LIFNeuron {

  private val synapses = new mutable.HashMap[ActorRef, Double]()
  private val synapsesLastSpikes = new mutable.HashMap[ActorRef, Long]()
  private val connectionOrder = new mutable.ListBuffer[ActorRef]()

  protected def getWeightOf(sender: ActorRef): Double = synapses(sender)
  protected def setWeightTo(node: ActorRef, newWeight: Double): Unit = synapses.update(node, newWeight)

  protected def getLastSpikeOf(sender: ActorRef): Long = synapsesLastSpikes.getOrElse(sender, Long.MinValue)
  protected def setLastSpikeOf(sender: ActorRef, t: Long): Unit = synapsesLastSpikes.update(sender, t)

  protected def getIngoingConnectionWeights() : Seq[Double] = {
    connectionOrder.map(n => synapses(n))
  }

  protected val minimalWeight = 0f
  protected val maximalWeight = 1f

  override def addIngoingSynapse(node: ActorRef): Unit = {
    val weight = math.max(this.minimalWeight, math.min(this.maximalWeight, QBGParameters.init_mean + Random.nextGaussian().toFloat * QBGParameters.init_std))
    connectionOrder += node
    synapses += (node -> weight)
  }

  protected def backfire(): Unit = {
    val newWeights = this.getIngoingConnections("default").map(node => {
      val oldWeight = this.getWeightOf(node).toFloat
      val newWeight = if (this.getLastSpikeOf(node) < this.getCurrentSimulationTime - QBGParameters.stdpWindow) {
        QBGFormula.decreaseWeight(oldWeight.toFloat, QBGParameters.alf_m, QBGParameters.g_min, QBGParameters.g_max, QBGParameters.beta_m)
      }
      else {
        QBGFormula.increaseWeight(oldWeight.toFloat, QBGParameters.alf_p, QBGParameters.g_min, QBGParameters.g_max, QBGParameters.beta_p)
      }
      (node, oldWeight, newWeight)
    })

    newWeights.foreach(result => {
      this.setWeightTo(result._1, result._3)
    })
  }
}

trait QBGSignedSynapse extends QBGUnsignedSynapse {

  override protected val minimalWeight = -1f
  override protected val maximalWeight = 1f

  override protected def backfire(): Unit = {
    val newWeights = this.getIngoingConnections("default").map(node => {
      val oldWeight = this.getWeightOf(node).toFloat
      val newWeight = if (this.getLastSpikeOf(node) < this.getCurrentSimulationTime - QBGParameters.stdpWindow) {
        QBGFormula.decreaseWeightWithNeg(oldWeight.toFloat, QBGParameters.alf_m, QBGParameters.g_min, QBGParameters.g_max, QBGParameters.beta_m)
      }
      else {
        QBGFormula.increaseWeightWithNeg(oldWeight.toFloat, QBGParameters.alf_p, QBGParameters.g_min, QBGParameters.g_max, QBGParameters.beta_p)
      }
      (node, oldWeight, newWeight)
    })


    newWeights.foreach(result => {
      this.setWeightTo(result._1, result._3)
    })
  }
}

abstract class LIFNeuronImpl extends NodeImpl with LIFNeuron {
  //Time t is in milliseconds

  private var fixedParameter = false
  private var membranePotential: Double = 0f
  private var membranePotentialThreshold: Double = 1f
  private var tRefract = 1000
  private var tInhibit = 10000
  private var tLeak = 100000

  private var theta: Double = 0f

  /********************************************************************************************************************
    * Remembered properties
    ******************************************************************************************************************/
  private var tLastTheta: Long = 0
  private var tLastInhib :Long = -tInhibit-1
  private var tLastSpike: Long = 0
  private var tLastFire: Long = -tRefract-1

  var spikesBeforeFire = 0

  /********************************************************************************************************************
    * Setters
    ******************************************************************************************************************/
  def setMembraneThresholdPotential(potential: Int): Unit = this.membranePotentialThreshold = potential

  /********************************************************************************************************************
    * Connection Management
    ******************************************************************************************************************/
  override def manageIngoingConnectionFrom(node: ActorRef): Unit ={
    this.addIngoingSynapse(node)
  }

  /********************************************************************************************************************
    * Message management
    ******************************************************************************************************************/
  def receiveMessage(message: Any, sender: ActorRef): Unit = {
    if (message == LIFNeuron.inhibitMessage){
      tLastInhib = this.getCurrentSimulationTime
      membranePotential = 0
      return
    }
    //TODO: println(s"[$this] Got message $message at t=${this.getCurrentSimulationTime()}")
    //We update the last spike time of the sender neuron
    //This is required to correctly calculate the QBGFormulas
    //Question: should be this done before of after checking for inhibition?
    this.setLastSpikeOf(sender, this.getCurrentSimulationTime)

    if(this.getCurrentSimulationTime - this.tLastFire < this.tRefract
    || this.getCurrentSimulationTime - this.tLastInhib < this.tInhibit){
      return
    }

    this.spikesBeforeFire += 1
    val weightOfEntrantConnection = this.getWeightOf(sender)
    this.membranePotential = this.membranePotential * exp(-(this.getCurrentSimulationTime - tLastSpike).toDouble / tLeak.toDouble).toFloat + weightOfEntrantConnection

    if(true) {//membraneThresholdType == MembraneThresholdTypeEnum.Dynamic) {
      this.theta = this.theta * exp(-(this.getCurrentSimulationTime - this.tLastTheta).toDouble / QBGParameters.thetaLeak).toFloat
      this.tLastTheta = this.getCurrentSimulationTime
    }
    //TODO: println(s"[$this] Membrane potential: $membranePotential, Threshold: ${membranePotentialThreshold+theta}")
    if(this.membranePotential >= this.membranePotentialThreshold + theta) {
      this.fire()
    }
    this.tLastSpike = this.getCurrentSimulationTime
  }

  def fire(): Unit = {
    println(s"[$this] Fire after receiving $spikesBeforeFire spikes!")
    this.membranePotential = 0
    this.spikesBeforeFire = 0
    if(true){//membraneThresholdType == MembraneThresholdTypeEnum.Dynamic) {
      this.theta += QBGParameters.theta_step
    }

    this.backfire()

    this.tLastFire = this.getCurrentSimulationTime

    this.broadcastMessage(this.getCurrentSimulationTime, LIFNeuron.inhibitMessage, LIFNeuron.inhibitRoleName)
    this.broadcastMessage(this.getCurrentSimulationTime, LIFNeuron.spikeMessage)
  }

  override def receive: Receive = super.receive orElse {
    case SetMembraneThresholdPotential(potential) => this.setMembraneThresholdPotential(potential)
    case GetIngoingConnectionWeights => sender ! this.getIngoingConnectionWeights()
  }
}

package fr.univ_lille.cristal.emeraude.chasqui.porting

import akka.actor.{ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import fr.univ_lille.cristal.emeraude.chasqui.core.Node.GetId
import fr.univ_lille.cristal.emeraude.chasqui.core.SynchronizerStrategy
import fr.univ_lille.cristal.emeraude.chasqui.core.synchronization.SynchronizerStrategyCompanion

import scala.collection.mutable
import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

class SimulationBuilder {

  val system = ActorSystem.create("digitalhex")
  implicit val executionContext = system.dispatcher

  var synchronizationStrategyBuilder: SynchronizerStrategyCompanion = null
  val layers = new mutable.HashSet[SimulationLayer]()

  def setSynchronizerStrategy(aSyncronizationStrategyBuilder: SynchronizerStrategyCompanion) = this.synchronizationStrategyBuilder = aSyncronizationStrategyBuilder

  def addNodeLayer(props: Props, numberOfNodes: Int = 1) = {
    val layer = new SimulationLayer(props, numberOfNodes)
    layers.add(layer)
    layer
  }

  def build() = {
    this.layers.foreach(layerBuilder => layerBuilder.buildOn(this))
    this.layers.foreach(layerBuilder => layerBuilder.connectOn(this))
  }
  def waitReady() = {
    implicit val timeout: Timeout = Timeout(5 seconds)
    val nodes = layers.foldLeft[Seq[TypedNeuron]](Seq())((accum, layer) => accum ++ layer.neurons)
    val readyFutures = nodes.map(node => (node.actor ? GetId).asInstanceOf[Future[Long]])
    Await.result(Future.sequence(readyFutures), timeout.duration)
  }

  def createActor(props: Props) = system.actorOf(props)
  def createSynchronizationStrategy(): SynchronizerStrategy = this.synchronizationStrategyBuilder.buildFor(system)
}

class SimulationLayer(props: Props, numberOfNodes: Int) {
  var baseId: String = _
  val connections = new mutable.HashSet[LayerConnection]()
  var neurons : Seq[TypedNeuron] = _

  def setBaseId(str: String): Unit = this.baseId = str
  def addConnection(connection: LayerConnection): Unit = this.connections.add(connection)


  def buildOn(builder: SimulationBuilder): Unit = {
    neurons = (0 until numberOfNodes).map(i =>{
      val neuron = builder.createActor(props)
      val neuronActorWrapper = new TypedNeuron(neuron)
      neuronActorWrapper.setId(s"${this.baseId}($i)")
      neuronActorWrapper.setSynchronizerStrategy(builder.createSynchronizationStrategy())
      neuronActorWrapper
    })
  }

  def connectOn(builder: SimulationBuilder): Unit = {
    this.connections.foreach(connections => connections.connectOn(this))
  }
}

object FullConnection {

  def connect(aLayer: SimulationLayer, anotherLayer: SimulationLayer, role: String = "default"): Unit = {
    aLayer.addConnection(new LayerConnection(anotherLayer, this, role))
  }

  def selfConnect(aLayer: SimulationLayer, role: String = "default"): Unit = {
    this.connect(aLayer, aLayer, role)
  }

  def createConnections(origin: SimulationLayer, targetLayer: SimulationLayer, role: String): Unit = {
    for (neuron1 <- origin.neurons; neuron2 <- targetLayer.neurons if neuron1 != neuron2){
      neuron1.blockingConnectTo(neuron2, role)
    }
  }
}

class LayerConnection(targetLayer: SimulationLayer, connectionKind: FullConnection.type, role: String){
  def connectOn(origin: SimulationLayer): Unit = {
    this.connectionKind.createConnections(origin, targetLayer, role)
  }
}
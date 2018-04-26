package fr.univ_lille.cristal.emeraude.chasqui.porting

import java.awt.event.ActionEvent
import javax.swing.AbstractAction

import akka.actor.{ActorRef, ActorSystem, Props}
import akka.pattern.ask
import akka.util.Timeout
import fr.univ_lille.cristal.emeraude.chasqui.core.Node.{GetId, NotifyFinishedQuantum}
import fr.univ_lille.cristal.emeraude.chasqui.core.NodeImpl
import fr.univ_lille.cristal.emeraude.chasqui.core.synchronization.GlobalSynchronizerWithLocalQueueStrategy
import fr.univ_lille.cristal.emeraude.chasqui.core.typed.TypedNode

import scala.concurrent.duration._
import scala.concurrent.{Await, Future}

/**
  * Created by guille on 19/04/17.
  */
object ExampleDigitalHexGlobalWithLookahead extends App {

  QBGParameters.alf_m = 0.02f
  QBGParameters.alf_p = 0.06f
  QBGParameters.beta_m = 3f
  QBGParameters.beta_p = 2f
  QBGParameters.stdpWindow = 50000

  implicit def typedNodeToSeqOfTypedNode(node: TypedNode): Seq[TypedNode] = Seq(node)

  private def fullConnection(layer1: Seq[TypedNode], layer2: Seq[TypedNode], role: String = "default"): Unit = {
    for (neuron1 <- layer1; neuron2 <- layer2 if neuron1 != neuron2){
      neuron1.blockingConnectTo(neuron2, role)
    }
  }

  val system = ActorSystem.create("digitalhex")
  implicit val executionContext = system.dispatcher
  val inputLayer = collection.mutable.ListBuffer[TypedInputNeuron]()
  val outputLayer = collection.mutable.ListBuffer[TypedNeuron]()

  val charactersToLearn = 0 to 4
  val oversampling = 3

  val numberOfPixels = (3 * oversampling) * (5 * oversampling)

  class Model extends LIFNeuronImpl with QBGSignedSynapse

  //Input layer
  for(i <- 0 to numberOfPixels - 1){
    val neuron = system.actorOf(Props[InputNeuronImpl]())
    val neuronActorWrapper = new TypedInputNeuron(neuron)
    neuronActorWrapper.setId(s"InputNeuron(${(i/ (3 * oversampling)) + 1},${Math.floorMod(i , (3*oversampling))})")
    neuronActorWrapper.setSynchronizerStrategy(new GlobalSynchronizerWithLocalQueueStrategy(system))
    inputLayer += neuronActorWrapper
  }

  //Input Actor
  val inputActor = system.actorOf(Props(new DigitalHexInputNode(charactersToLearn, 1000, oversampling)))
  val typedInputActor = new TypedNode(inputActor)
  typedInputActor.setId("Input")
  typedInputActor.setSynchronizerStrategy(new GlobalSynchronizerWithLocalQueueStrategy(system))

  //Output layer
  for(i <- 1 to charactersToLearn.size + 4){
    val neuron = system.actorOf(Props[Model]())
    val neuronActorWrapper = new TypedNeuron(neuron)
    neuronActorWrapper.setId(s"Neuron($i)")
    neuronActorWrapper.setMembraneThresholdPotential(2 * oversampling * oversampling)
    outputLayer += neuronActorWrapper
  }

  (inputLayer ++ outputLayer).foreach( neuron => {
    neuron.setSynchronizerStrategy(new GlobalSynchronizerWithLocalQueueStrategy(system))
  })

  this.fullConnection(Seq(typedInputActor), inputLayer)
  this.fullConnection(inputLayer, outputLayer)
  this.fullConnection(outputLayer, outputLayer, LIFNeuron.inhibitRoleName)

  val controllerNodeActor = system.actorOf(Props(new NodeImpl() {
    override def receiveMessage(message: Any, sender: ActorRef): Unit = {
      //Nothing
    }
  }))
  val typedController = new TypedNode(controllerNodeActor)
  typedController.setSynchronizerStrategy(new GlobalSynchronizerWithLocalQueueStrategy(system))

  implicit val timeout: Timeout = Timeout(5 seconds)
  val nodes = Seq(typedController, typedInputActor) ++ inputLayer ++ outputLayer
  val readyFutures = nodes.map(node => (node.actor ? GetId).asInstanceOf[Future[Long]])
  Await.result(Future.sequence(readyFutures), timeout.duration)

  new NodeInspector(nodes)
    .addButton("Advance", new AbstractAction() {
      override def actionPerformed(e: ActionEvent): Unit = {
        controllerNodeActor ! NotifyFinishedQuantum
      }
    })
    .addButton("Start", new AbstractAction() {
      override def actionPerformed(e: ActionEvent): Unit = {
        //Start the simulation
        nodes.foreach( neuron => {
          neuron.start()
        })

        //Open the visualization
        system.actorOf(Props(new SynapsesWeightGraphActor(
          outputLayer,
          outputLayer.size - 1,
          10,
          10,
          1000/24,
          null,
          3 * oversampling
        )))
      }
    })
    .addButton("Exit", new AbstractAction() {
      override def actionPerformed(e: ActionEvent): Unit = {
        System.exit(0)
      }
    })
    .open()
}

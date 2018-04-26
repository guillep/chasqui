package fr.univ_lille.cristal.emeraude.chasqui.porting

import java.awt.event.ActionEvent
import javax.swing.AbstractAction

import akka.actor.{ActorRef, Props}
import fr.univ_lille.cristal.emeraude.chasqui.core.Node.NotifyFinishedQuantum
import fr.univ_lille.cristal.emeraude.chasqui.core.NodeImpl
import fr.univ_lille.cristal.emeraude.chasqui.core.synchronization.GlobalSynchronizerWithLocalQueueStrategy
import fr.univ_lille.cristal.emeraude.chasqui.core.typed.TypedNode

/**
  * Created by guille on 19/04/17.
  */
object Example extends App {

  QBGParameters.alf_m = 0.02f
  QBGParameters.alf_p = 0.06f
  QBGParameters.beta_m = 3f
  QBGParameters.beta_p = 2f
  QBGParameters.stdpWindow = 50000

  implicit def typedNodeToSeqOfTypedNode(node: TypedNode): Seq[TypedNode] = Seq(node)
  val charactersToLearn = 0 to 4
  val oversampling = 3
  val numberOfPixels = (3 * oversampling) * (5 * oversampling)
  class Model extends LIFNeuronImpl with QBGSignedSynapse
  val synchronizationStrategy = GlobalSynchronizerWithLocalQueueStrategy

  val builder = new SimulationBuilder
  val layer0 = builder.addNodeLayer(Props(new DigitalHexInputNode(charactersToLearn, 1000, oversampling)))
  val layer1 = builder.addNodeLayer(Props[InputNeuronImpl](), numberOfPixels)
  val layer2 = builder.addNodeLayer(Props[Model](), charactersToLearn.size)

  FullConnection.connect(layer0, layer1)
  FullConnection.connect(layer1, layer2)
  FullConnection.selfConnect(layer2, LIFNeuron.inhibitRoleName)

  layer0.setBaseId("Input")
  layer1.setBaseId("InputNeuron")
  layer2.setBaseId("OutputNeuron")

  builder.setSynchronizerStrategy(synchronizationStrategy)
  builder.build()

  layer1.neurons.foreach(neuron =>
    new TypedNeuron(neuron.actor).setMembraneThresholdPotential(2 * oversampling * oversampling))

  val controllerNodeActor = builder.system.actorOf(Props(new NodeImpl() {
    override def receiveMessage(message: Any, sender: ActorRef): Unit = {
      //Nothing
    }
  }))
  val typedController = new TypedNode(controllerNodeActor)
  typedController.setSynchronizerStrategy(synchronizationStrategy, builder.system)

  builder.waitReady()

  val nodes = builder.layers.foldLeft[Seq[TypedNode]](Seq())((accum, each) => accum ++ each.neurons)
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
        builder.system.actorOf(Props(new SynapsesWeightGraphActor(
          layer2.neurons,
          layer2.neurons.size - 1,
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

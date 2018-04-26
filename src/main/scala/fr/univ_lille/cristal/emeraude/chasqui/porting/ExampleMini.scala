package fr.univ_lille.cristal.emeraude.chasqui.porting

import java.awt.event.ActionEvent
import javax.swing.AbstractAction

import akka.actor.{ActorRef, ActorSystem, Props}
import fr.univ_lille.cristal.emeraude.chasqui.core.NodeImpl
import fr.univ_lille.cristal.emeraude.chasqui.core.synchronization.GlobalSynchronizerWithLocalQueueStrategy
import fr.univ_lille.cristal.emeraude.chasqui.core.typed.TypedNode

/**
  * Created by guille on 19/04/17.
  */
object ExampleMini extends App {

  implicit def typedNodeToSeqOfTypedNode(node: TypedNode): Seq[TypedNode] = Seq(node)

  private def fullConnection(layer1: Seq[TypedNode], layer2: Seq[TypedNode], role: String = "default"): Unit = {
    for (neuron1 <- layer1; neuron2 <- layer2 if neuron1 != neuron2){
      neuron1.blockingConnectTo(neuron2, role)
    }
  }

  val system = ActorSystem.create("digitalhex")
  implicit val executionContext = system.dispatcher

  val input = new TypedNode(system.actorOf(Props(new NodeImpl() {

    override def advanceSimulationTime(newQuantum: Long): Unit = {
      super.advanceSimulationTime(newQuantum)
      if (newQuantum == 0) {
        (1 to 10).foreach( t => this.broadcastMessage(t, "Input" + newQuantum))
      }
      this.notifyFinishedQuantum()
    }

    override def receiveMessage(message: Any, sender: ActorRef): Unit = {
      //Nothing
    }
  })))

  val nodeA = new TypedNode(system.actorOf(Props(new NodeImpl() {
    override def receiveMessage(message: Any, sender: ActorRef): Unit = {
      val t = this.getCurrentSimulationTime
      this.broadcastMessage(t + 1, message + "-NodeA" + t)
    }
  })))

  val nodeB = new TypedNode(system.actorOf(Props(new NodeImpl() {
    override def receiveMessage(message: Any, sender: ActorRef): Unit = {
      println(message + "-NodeB" + this.getCurrentSimulationTime)
    }
  })))

  input.blockingConnectTo(nodeA, "default")
  nodeA.blockingConnectTo(nodeB, "default")

  Thread.sleep(500)

  input.setSynchronizerStrategy(new GlobalSynchronizerWithLocalQueueStrategy(system))
  input.setId("Input")
  nodeA.setSynchronizerStrategy(new GlobalSynchronizerWithLocalQueueStrategy(system))
  nodeA.setId("NodeA")
  nodeB.setSynchronizerStrategy(new GlobalSynchronizerWithLocalQueueStrategy(system))
  nodeB.setId("NodeB")

  val nodes = Seq(input, nodeA, nodeB)
  new NodeInspector(nodes)
    .addButton("Start", new AbstractAction() {
      override def actionPerformed(e: ActionEvent): Unit = {
        //Start the simulation
        nodes.foreach( node => {
          node.start()
        })
      }
    })
    .open()
}

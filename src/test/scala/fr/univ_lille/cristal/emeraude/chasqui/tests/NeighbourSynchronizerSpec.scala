package fr.univ_lille.cristal.emeraude.chasqui.tests

import fr.univ_lille.cristal.emeraude.chasqui.core.synchronization.NeighbourSynchronizerStrategyWithLookahead

/**
  * Created by guille on 12/06/17.
  */
class NeighbourSynchronizerSpec extends ChasquiBaseSpec {
  "A node with a single ingoing neighbour" should "advance to the neighbour quantum" in {
    val nodeA = newNode("NodeA")
    val nodeB = newNode("NodeB")

    //Setup connection NodeA -> NodeB
    nodeA.connectTo(nodeB)

    nodeA.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
    nodeB.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())

    //Wait until all connections are set up
    Thread.sleep(1000)

    //NodeB is at T=0 and schedules a message in the future T=17
    nodeA.setTime(0); nodeB.setTime(0)
    nodeA.scheduleMessage("message", 17, nodeA)
    nodeA.notifyFinishedQuantum()

    //Since NodeB, only outgoing neighbour of NodeA, is finished => NodeA can advance his simulation time
    Thread.sleep(1000)
    nodeB.getCurrentSimulationTime() should be(17)
  }

  "Several nodes with a single ingoing neighbours" should "advance to the neighbour quantum" in {
    val nodeA = newNode("NodeA")
    val nodeB = newNode("NodeB")
    val nodeC = newNode("NodeC")

    //Setup connection NodeA -> NodeB
    nodeA.connectTo(nodeB)
    nodeA.connectTo(nodeC)

    nodeA.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
    nodeB.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
    nodeC.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())

    //Wait until all connections are set up
    Thread.sleep(1000)

    //NodeB is at T=0 and schedules a message in the future T=17
    nodeA.setTime(0); nodeB.setTime(0); nodeC.setTime(0)
    nodeA.scheduleMessage("message", 17, nodeA)
    nodeA.notifyFinishedQuantum()

    //Since NodeB, only outgoing neighbour of NodeA, is finished => NodeA can advance his simulation time
    Thread.sleep(1000)
    nodeB.getCurrentSimulationTime() should be(17)
    nodeC.getCurrentSimulationTime() should be(17)
  }

  "A node with a several finished ingoing neighbours" should "advance to the neighbour quantum" in {
    val nodeA = newNode("NodeA")
    val nodeB = newNode("NodeB")
    val nodeC = newNode("NodeC")

    //Setup connections
    nodeA.connectTo(nodeC)
    nodeB.connectTo(nodeC)

    nodeA.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
    nodeB.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
    nodeC.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())

    //Wait until all connections are set up
    Thread.sleep(1000)

    //NodeB is at T=0 and schedules a message in the future T=17
    nodeA.setTime(0); nodeB.setTime(0); nodeC.setTime(0)

    nodeA.scheduleMessage("message", 17, nodeA)
    nodeA.notifyFinishedQuantum()

    nodeB.scheduleMessage("message", 17, nodeA)
    nodeB.notifyFinishedQuantum()

    //Since NodeB, only outgoing neighbour of NodeA, is finished => NodeA can advance his simulation time
    Thread.sleep(1000)
    nodeC.getCurrentSimulationTime() should be(17)
  }

  "A node with a several finished ingoing neighbours" should "advance to the minimum neighbour quantum" in {
    val nodeA = newNode("NodeA")
    val nodeB = newNode("NodeB")
    val nodeC = newNode("NodeC")

    //Setup connections
    nodeA.connectTo(nodeC)
    nodeB.connectTo(nodeC)

    nodeA.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
    nodeB.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
    nodeC.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())

    //Wait until all connections are set up
    Thread.sleep(1000)

    //NodeB is at T=0 and schedules a message in the future T=17
    nodeA.setTime(0); nodeB.setTime(0); nodeC.setTime(0)

    nodeA.scheduleMessage("message", 17, nodeA)
    nodeA.notifyFinishedQuantum()

    nodeB.scheduleMessage("message", 9, nodeA)
    nodeB.notifyFinishedQuantum()

    //Since NodeB, only outgoing neighbour of NodeA, is finished => NodeA can advance his simulation time
    Thread.sleep(1000)
    nodeC.getCurrentSimulationTime() should be(9)
  }

  "A node with several ingoing neighbours" should "not advance if one neighbour is not finished" in {
    val nodeA = newNode("NodeA")
    val nodeB = newNode("NodeB")
    val nodeC = newNode("NodeC")

    //Setup connections
    nodeA.connectTo(nodeC)
    nodeB.connectTo(nodeC)

    nodeA.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
    nodeB.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
    nodeC.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())

    //Wait until all connections are set up
    Thread.sleep(1000)

    //NodeB is at T=0 and schedules a message in the future T=17
    nodeA.setTime(0); nodeB.setTime(0); nodeC.setTime(0)

    nodeA.scheduleMessage("message", 17, nodeA)
    nodeA.notifyFinishedQuantum()

    //Since NodeB, only outgoing neighbour of NodeA, is finished => NodeA can advance his simulation time
    Thread.sleep(1000)
    nodeC.getCurrentSimulationTime() should be(0)
  }

  "A node with several ingoing neighbours" should "advance if one neighbour was already finished before" in {
    val nodeA = newNode("NodeA")
    val nodeB = newNode("NodeB")
    val nodeC = newNode("NodeC")

    //Setup connections
    nodeA.connectTo(nodeC)
    nodeB.connectTo(nodeC)

    nodeA.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
    nodeB.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
    nodeC.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())

    //Wait until all connections are set up
    Thread.sleep(1000)

    //NodeB is at T=0 and schedules a message in the future T=17
    nodeA.setTime(0); nodeB.setTime(0); nodeC.setTime(0)

    nodeA.scheduleMessage("message", 17, nodeA)
    nodeA.advanceSimulationTime(0)
    nodeA.advanceSimulationTime(9)

    nodeB.scheduleMessage("message", 9, nodeA)
    nodeB.scheduleMessage("message", 17, nodeA)
    nodeB.processNextQuantum()

    //Since NodeB, only outgoing neighbour of NodeA, is finished => NodeA can advance his simulation time
    Thread.sleep(1000)
    nodeC.getCurrentSimulationTime() should be(9)

    nodeB.advanceSimulationTime(9)
    Thread.sleep(1000)
    nodeC.getCurrentSimulationTime() should be(17)
  }
}

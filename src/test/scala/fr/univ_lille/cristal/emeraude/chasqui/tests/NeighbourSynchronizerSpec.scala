package fr.univ_lille.cristal.emeraude.chasqui.tests

import fr.univ_lille.cristal.emeraude.chasqui.core.synchronization.{FinishedQuantumWithLookahead, NeighbourSynchronizerStrategyWithLookahead}

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

    Thread.sleep(1000)
    nodeC.getCurrentSimulationTime() should be(0)
  }

  "A node with several ingoing neighbours"
  ignore should "advance if one neighbour was already finished before" in {

    val nodeA = newNode("NodeA", false)
    val nodeB = newNode("NodeB", false)
    val nodeC = newNode("NodeC", false)

    //Setup connections
    nodeA.connectTo(nodeC)
    nodeB.connectTo(nodeC)

    nodeA.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
    nodeB.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
    nodeC.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())

    //NodeB is at T=0 and schedules a message in the future T=17
    nodeA.setTime(0); nodeB.setTime(0); nodeC.setTime(0)

    //Wait until all connections are set up
    Thread.sleep(1000)
    nodeC.start()

    //We should advance the node at least to 0 and 9
    //First advancing it to 0 tells NodeC that 0 is finished so he can advance to 9
    //Second advancing to 9 tells NodeC that 9 is finished so he can advance to 17
    nodeA.start()
    nodeA.scheduleMessage("message", 17, nodeA)

    Thread.sleep(1000)

    nodeB.start()
    nodeB.scheduleMessage("message", 9, nodeA)


    Thread.sleep(1000)
    nodeC.getCurrentSimulationTime() should be(9)

    nodeB.scheduleMessage("message", 17, nodeA)

    Thread.sleep(1000)
    nodeC.getCurrentSimulationTime() should be(17)
  }

  "A node with several ingoing neighbours"
  ignore should "advance if one neighbour was already finished and no other neighbours have messages" in {
    val nodeA = newNode("NodeA", false)
    val nodeB = newNode("NodeB", false)
    val nodeC = newNode("NodeC", false)

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

    nodeA.processNextQuantum()

    nodeB.scheduleMessage("message", 9, nodeA)
    nodeB.scheduleMessage("message", 17, nodeA)
    nodeB.processNextQuantum()

    Thread.sleep(1000)
    nodeC.getCurrentSimulationTime() should be(9)

    nodeB.advanceSimulationTime(9)
    nodeB.processNextQuantum()
    Thread.sleep(1000)
    nodeC.getCurrentSimulationTime() should be(17)
  }

  "A node with pending messages" should "advance to min(pending,incoming)" in {
    val nodeA = newNode("NodeA")
    val nodeB = newNode("NodeB")

    //Setup connection NodeA -> NodeB
    nodeA.connectTo(nodeB)

    nodeA.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
    nodeB.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())

    //Wait until all connections are set up
    Thread.sleep(1000)

    nodeB.scheduleMessage("message", 7, nodeA)

    //NodeB is at T=0 and schedules a message in the future T=17
    nodeA.setTime(0); nodeB.setTime(0)
    nodeA.scheduleMessage("message", 17, nodeA)
    nodeA.notifyFinishedQuantum()

    Thread.sleep(1000)
    nodeB.getCurrentSimulationTime() should be(7)
  }

  "A node cycle"
  ignore should "advance together" in {
    val nodeA = newNode("NodeA", false)
    val nodeB = newNode("NodeB", false)

    //Setup connection NodeA <-> NodeB
    nodeA.connectTo(nodeB)
    nodeB.connectTo(nodeA)

    nodeA.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
    nodeB.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())

    //Wait until all connections are set up
    Thread.sleep(1000)

    nodeA.setTime(0); nodeB.setTime(0)

    nodeA.scheduleMessage("message", 17, nodeA)
    nodeB.scheduleMessage("message", 7, nodeA)

    nodeA.advanceSimulationTime(0)
    nodeA.processNextQuantum()
    nodeB.advanceSimulationTime(0)
    nodeB.processNextQuantum()

    Thread.sleep(1000)
    nodeA.getCurrentSimulationTime() should be(7)
    nodeB.getCurrentSimulationTime() should be(7)
  }

  //Tests for FinishedQuantumWithLookahead
  "A default FinishedQuantumWithLookahead" should "not have finished the quantum at any t >= 0" in {
    (1 to 100).foreach( t =>
      new FinishedQuantumWithLookahead().hasFinishedQuantum(t) should not be(true)
    )
  }

  "A FinishedQuantumWithLookahead with no more messages" should "be have finished quantum" in {
    new FinishedQuantumWithLookahead(10).hasFinishedQuantum(10) should be(true)
  }
}

package fr.univ_lille.cristal.emeraude.chasqui.tests

import fr.univ_lille.cristal.emeraude.chasqui.core.GlobalSynchronizerStrategy

/**
  * Created by guille on 10/04/17.
  */
class GlobalSynchronizerSpec extends ChasquiBaseSpec {

  "A single node with global synchronizer strategy" should "advance in time until no more messages are available" in {
    val nodeA = newNode
    val nodeB = newNode

    nodeA.setSynchronizerStrategy(new GlobalSynchronizerStrategy(system))

    //Send three messages in the future
    nodeB.sendMessage(nodeA, 1, "test")
    nodeB.sendMessage(nodeA, 2, "test2")
    nodeB.sendMessage(nodeA, 3, "test3")

    //This will process all message in time 1 and inform it finished to its synchronizer strategy
    nodeA.start()

    //We sleep here to wait the actor system to react
    //This is not the best way to test, because it may not scale in the future
    // but it's a practical and simple one to start with
    Thread.sleep(500)
    nodeA.getCurrentSimulationTime() should be(3)
  }

  "Several nodes with global synchronizer strategy" should "advance in time until no more messages are available" in {
    val nodeA = newNode
    val nodeB = newNode

    nodeA.setSynchronizerStrategy(new GlobalSynchronizerStrategy(system))
    nodeB.setSynchronizerStrategy(new GlobalSynchronizerStrategy(system))

    //Send three messages in the future
    nodeB.sendMessage(nodeA, 1, "test")
    nodeA.sendMessage(nodeB, 2, "response")
    nodeB.sendMessage(nodeA, 2, "test2")
    nodeB.sendMessage(nodeA, 3, "test3")

    //This will process all message in time 1 and inform it finished to its synchronizer strategy
    nodeA.start()
    nodeB.start()

    //We sleep here to wait the actor system to react
    //This is not the best way to test, because it may not scale in the future
    // but it's a practical and simple one to start with
    Thread.sleep(500)

    nodeA.getCurrentSimulationTime() should be(3)
    nodeB.getCurrentSimulationTime() should be(3)
  }

  "Several nodes with global synchronizer strategy" should "advance when only one gets messages" in {
    val nodeA = newNode
    val nodeB = newNode

    nodeA.setSynchronizerStrategy(new GlobalSynchronizerStrategy(system))
    nodeB.setSynchronizerStrategy(new GlobalSynchronizerStrategy(system))

    //Send three messages in the future
    nodeB.sendMessage(nodeA, 1, "test")
    nodeB.sendMessage(nodeA, 2, "test2")
    nodeB.sendMessage(nodeA, 3, "test3")

    //This will process all message in time 1 and inform it finished to its synchronizer strategy
    nodeA.start()
    nodeB.start()

    //We sleep here to wait the actor system to react
    //This is not the best way to test, because it may not scale in the future
    // but it's a practical and simple one to start with
    Thread.sleep(500)

    nodeA.getCurrentSimulationTime() should be(3)
    nodeB.getCurrentSimulationTime() should be(3)
  }
}

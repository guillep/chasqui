package fr.univ_lille.cristal.emeraude.chasqui.tests

import fr.univ_lille.cristal.emeraude.chasqui.core.GlobalSynchronizerStrategy

/**
  * Created by guille on 10/04/17.
  */
class GlobalSynchronizerSpec extends ChasquiBaseSpec {

  "Two global synchronizer strategies" should "use the same global synchronizer actor" in {
    val strategy1 = new GlobalSynchronizerStrategy(system)
    val strategy2 = new GlobalSynchronizerStrategy(system)

    strategy1.getSynchronizerActor() should be(strategy2.getSynchronizerActor())
  }

  "A single node with global synchronizer strategy" should "advance in time until no more messages are available" in {
    val nodeA = newNode
    val nodeB = newNode

    nodeA.setSynchronizerStrategy(new GlobalSynchronizerStrategy(system))
    nodeB.setSynchronizerStrategy(new GlobalSynchronizerStrategy(system))

    //Send three messages in the future
    nodeB.sendMessage(nodeA, 1, "test")
    nodeB.sendMessage(nodeA, 2, "test2")
    nodeB.sendMessage(nodeA, 3, "test3")

    //This will process all message in time 1 and inform it finished to its synchronizer strategy
    nodeA.checkPendingMessagesInQueue()
    nodeB.checkPendingMessagesInQueue()

    //We sleep here to wait the actor system to react
    //This is not the best way to test, because it may not scale in the future
    // but it's a practical and simple one to start with
    Thread.sleep(1000)
    nodeB.getCurrentSimulationTime() should be(3)
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
    nodeA.checkPendingMessagesInQueue()
    nodeB.checkPendingMessagesInQueue()

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
    nodeA.checkPendingMessagesInQueue()
    nodeB.checkPendingMessagesInQueue()

    //We sleep here to wait the actor system to react
    //This is not the best way to test, because it may not scale in the future
    // but it's a practical and simple one to start with
    Thread.sleep(500)

    nodeA.getCurrentSimulationTime() should be(3)
    nodeB.getCurrentSimulationTime() should be(3)
  }

  "A globally synchronized node" should "not advance until all others advance" in {
    val nodeA = newNode
    val nodeB = newNode

    nodeA.setSynchronizerStrategy(new GlobalSynchronizerStrategy(system))
    nodeB.setSynchronizerStrategy(new GlobalSynchronizerStrategy(system))

    //Send three messages in the future
    nodeB.sendMessage(nodeA, 1, "test")
    nodeB.sendMessage(nodeA, 2, "test2")
    nodeB.sendMessage(nodeA, 3, "test3")

    //This will process all message in time 1 and inform it finished to its synchronizer strategy
    nodeA.checkPendingMessagesInQueue()

    //We sleep here to wait the actor system to react
    //This is not the best way to test, because it may not scale in the future
    // but it's a practical and simple one to start with
    Thread.sleep(500)

    nodeA.getCurrentSimulationTime() should be(0)
  }

  "A globally synchronized node" should "advance as soon as all others advance" in {
    val nodeA = newNode
    val nodeB = newNode

    nodeA.setSynchronizerStrategy(new GlobalSynchronizerStrategy(system))
    nodeB.setSynchronizerStrategy(new GlobalSynchronizerStrategy(system))

    //Send three messages in the future
    nodeB.sendMessage(nodeA, 1, "test")
    nodeB.sendMessage(nodeA, 2, "test2")
    nodeB.sendMessage(nodeA, 3, "test3")

    //This will process all message in time 1 and inform it finished to its synchronizer strategy
    nodeA.checkPendingMessagesInQueue()

    //We sleep here to wait the actor system to react
    //This is not the best way to test, because it may not scale in the future
    // but it's a practical and simple one to start with
    Thread.sleep(500)

    nodeB.checkPendingMessagesInQueue()

    //We sleep here to wait the actor system to react
    //This is not the best way to test, because it may not scale in the future
    // but it's a practical and simple one to start with
    Thread.sleep(500)
    nodeA.getCurrentSimulationTime() should be(3)
  }
}

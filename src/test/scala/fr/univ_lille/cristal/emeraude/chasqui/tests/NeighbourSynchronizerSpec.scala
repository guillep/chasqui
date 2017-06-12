package fr.univ_lille.cristal.emeraude.chasqui.tests

import fr.univ_lille.cristal.emeraude.chasqui.core.synchronization.{FinishedQuantum, GlobalSynchronizerStrategy, NeighbourSynchronizerStrategy}

/**
  * Created by guille on 12/06/17.
  */
class NeighbourSynchronizerSpec extends ChasquiBaseSpec {

  "A single node" should "not advance to next quantum if its neighbour is not ready" in {
    val nodeA = newNode
    val nodeB = newNode

    nodeA.setSynchronizerStrategy(new NeighbourSynchronizerStrategy())
    nodeB.setSynchronizerStrategy(new NeighbourSynchronizerStrategy())

    //Setup connection NodeA -> NodeB
    nodeA.connectTo(nodeB)

    //Send three messages in the future
    nodeA.sendMessage(nodeB, 0, "test")
    nodeA.sendMessage(nodeB, 1, "test1")
    nodeA.sendMessage(nodeB, 2, "test2")
    nodeA.sendMessage(nodeB, 3, "test3")

    //This will process all message in time 1
    //However, nodeA never starts/finishes the quantum
    nodeB.checkPendingMessagesInQueue()

    //We sleep here to wait the actor system to react
    //This is not the best way to test, because it may not scale in the future
    // but it's a practical and simple one to start with
    Thread.sleep(1000)
    nodeB.getCurrentSimulationTime() should be(0)
  }

  "A single node" should "advance to next quantum if its neighbour finished current quantum" in {
    val nodeSender = newNode("SENDER")
    val nodeReceiver = newNode("RECEIVER")
    val nodeBlocker = newNode("Blocker")

    nodeSender.setSynchronizerStrategy(new NeighbourSynchronizerStrategy())
    nodeReceiver.setSynchronizerStrategy(new NeighbourSynchronizerStrategy())

    //Setup connection NodeA <-> NodeB
    nodeSender.connectTo(nodeReceiver)
    nodeReceiver.connectTo(nodeSender)
    nodeBlocker.connectTo(nodeSender)

    //Send message to A so A stays in t=1
    nodeReceiver.sendMessage(nodeSender, 1, "test")

    //Send messages in the future for processing
    nodeSender.sendMessage(nodeReceiver, 0, "test")
    nodeSender.sendMessage(nodeReceiver, 1, "test1")
    nodeSender.sendMessage(nodeReceiver, 2, "test2")
    nodeSender.sendMessage(nodeReceiver, 3, "test3")

    //This will process all message in time 1
    //However, nodeA never starts/finishes the quantum
    nodeSender.broadcastMessageToIncoming(FinishedQuantum(0), 0)
    nodeReceiver.checkPendingMessagesInQueue()

    //We sleep here to wait the actor system to react
    //This is not the best way to test, because it may not scale in the future
    // but it's a practical and simple one to start with
    Thread.sleep(1000)
    nodeReceiver.getCurrentSimulationTime() should be(1)
  }

  "A single node" should "advance to next quantum several times if its neighbour finished current quantum" in {
    val nodeSender = newNode("SENDER")
    val nodeReceiver = newNode("RECEIVER")
    val nodeBlocker = newNode("Blocker")

    nodeSender.setSynchronizerStrategy(new NeighbourSynchronizerStrategy())
    nodeReceiver.setSynchronizerStrategy(new NeighbourSynchronizerStrategy())

    //Setup connection NodeA <-> NodeB
    nodeSender.connectTo(nodeReceiver)
    nodeReceiver.connectTo(nodeSender)
    nodeBlocker.connectTo(nodeSender)

    //Send message to A so A stays in t=1
    nodeReceiver.sendMessage(nodeSender, 1, "test")

    //Send messages in the future for processing
    nodeSender.sendMessage(nodeReceiver, 0, "test")
    nodeSender.sendMessage(nodeReceiver, 1, "test1")
    nodeSender.sendMessage(nodeReceiver, 2, "test2")
    nodeSender.sendMessage(nodeReceiver, 3, "test3")
    nodeSender.sendMessage(nodeReceiver, 4, "test4")
    nodeSender.sendMessage(nodeReceiver, 5, "test5")

    //This will process all message in time 1
    //However, nodeA never starts/finishes the quantum
    nodeSender.broadcastMessageToIncoming(FinishedQuantum(0), 0)
    nodeSender.broadcastMessageToIncoming(FinishedQuantum(1), 1)
    nodeSender.broadcastMessageToIncoming(FinishedQuantum(2), 2)
    nodeReceiver.checkPendingMessagesInQueue()

    //We sleep here to wait the actor system to react
    //This is not the best way to test, because it may not scale in the future
    // but it's a practical and simple one to start with
    Thread.sleep(1000)
    nodeReceiver.getCurrentSimulationTime() should be(3)
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
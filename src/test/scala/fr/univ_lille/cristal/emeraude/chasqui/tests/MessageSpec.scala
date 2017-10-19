package fr.univ_lille.cristal.emeraude.chasqui.tests

import fr.univ_lille.cristal.emeraude.chasqui.core.CausalityErrorStrategy
import org.mockito.Mockito.verify

/**
  * This class tests how messages flow between nodes.
  * This tests do not test node synchronization.
  * Node synchronization is done manually.
  */
class MessageSpec extends ChasquiBaseSpec {

  "A message from A to B in current simulation time t" should "arrive to B" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB.actor, 1, "message")

    Thread.sleep(500)
    nodeB.getReceivedMessages should contain ("message")
  }

  "A message from A to B in future time t+1" should "not arrive to B right away" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB.actor, 2, "message")

    Thread.sleep(500)
    nodeB.getReceivedMessages should be ('empty)
  }

  "A message from A to B in future time t+1" should "arrive to B after B advances its simulation time" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB.actor, 2, "message")

    Thread.sleep(500)
    nodeB.advanceSimulationTime(2)
    nodeB.processNextQuantum()
    nodeB.getReceivedMessages should contain ("message")
  }

  "A message from A to B in future time t+2" should "not arrive to B if B advances its simulation time only once" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB.actor, 3, "message")

    Thread.sleep(500)
    nodeB.advanceSimulationTime(1)
    nodeB.getReceivedMessages should be ('empty)
  }

  "A message from A to B in future time t+2" should "arrive to B after B advances its simulation time twice" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB.actor, 3, "message")

    Thread.sleep(500)
    nodeB.advanceSimulationTime(2)
    nodeB.processNextQuantum()
    nodeB.getReceivedMessages should not contain ("message")
    nodeB.advanceSimulationTime(3)
    nodeB.processNextQuantum()
    nodeB.getReceivedMessages should contain ("message")
  }

  "A message from A to B in past time t-1" should "not arrive to B" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB.actor, 0, "message")

    Thread.sleep(500)
    nodeB.getReceivedMessages should be ('empty)
  }

  "A message from A to B in past time t-1" should "not be scheduled" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB.actor, 0, "message")

    Thread.sleep(500)
    nodeB.getScheduledMessages should be ('empty)
  }

  "A message from A to B in past time t-1" should "be handled by a causality error strategy" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    val causalityErrorStrategy = mock[CausalityErrorStrategy]
    nodeB.setCausalityErrorStrategy(causalityErrorStrategy)

    nodeA.sendMessage(nodeB.actor, 0, "message")

    Thread.sleep(500)
    verify(causalityErrorStrategy).handleCausalityError(
      org.mockito.ArgumentMatchers.eq(0L),
      org.mockito.ArgumentMatchers.eq(1L),
      org.mockito.ArgumentMatchers.any(),
      org.mockito.ArgumentMatchers.eq(nodeA.actor),
      org.mockito.ArgumentMatchers.eq("message"))
  }
}

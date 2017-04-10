package fr.univ_lille.cristal.emeraude.chasqui.tests

import fr.univ_lille.cristal.emeraude.chasqui.core.CausalityErrorStrategy
import org.mockito.Mockito.verify

/**
  * Created by guille on 10/04/17.
  */
class MessageSpec extends ChasquiBaseSpec {

  "A message from A to B in current simulation time t" should "arrive to B" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB, 1, "message")

    nodeB.getReceivedMessages should contain ("message")
  }

  "A message from A to B in future time t+1" should "not arrive to B right away" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB, 2, "message")

    nodeB.getReceivedMessages should be ('empty)
  }

  "A message from A to B in future time t+1" should "arrive to B after B advances its simulation time" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB, 2, "message")

    nodeB.advanceSimulationTime()
    nodeB.getReceivedMessages should contain ("message")
  }

  "A message from A to B in future time t+2" should "not arrive to B if B advances its simulation time only once" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB, 3, "message")

    nodeB.advanceSimulationTime()
    nodeB.getReceivedMessages should be ('empty)
  }

  "A message from A to B in future time t+2" should "arrive to B after B advances its simulation time twice" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB, 3, "message")

    nodeB.advanceSimulationTime()
    nodeB.advanceSimulationTime()
    nodeB.getReceivedMessages should contain ("message")
  }

  "A message from A to B in past time t-1" should "not arrive to B" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB, 0, "message")

    nodeB.getReceivedMessages should be ('empty)
  }

  "A message from A to B in past time t-1" should "not be scheduled" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB, 0, "message")

    nodeB.getScheduledMessages should be ('empty)
  }

  "A message from A to B in past time t-1" should "be handled by a causality error strategy" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    val causalityErrorStrategy = mock[CausalityErrorStrategy]
    nodeB.setCausalityErrorStrategy(causalityErrorStrategy)

    nodeA.sendMessage(nodeB, 0, "message")

    verify(causalityErrorStrategy).handleCausalityError(0, 1, nodeB, nodeA, "message")
  }
}

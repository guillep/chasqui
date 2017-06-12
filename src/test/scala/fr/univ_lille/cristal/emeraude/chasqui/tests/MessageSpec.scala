package fr.univ_lille.cristal.emeraude.chasqui.tests

import fr.univ_lille.cristal.emeraude.chasqui.core.{CausalityErrorStrategy, NodeActorWrapper}
import org.mockito.Mockito.verify
import org.mockito.ArgumentCaptor

/**
  * Created by guille on 10/04/17.
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
    nodeB.advanceSimulationTime()
    nodeB.getReceivedMessages should contain ("message")
  }

  "A message from A to B in future time t+2" should "not arrive to B if B advances its simulation time only once" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB.actor, 3, "message")

    Thread.sleep(500)
    nodeB.advanceSimulationTime()
    nodeB.getReceivedMessages should be ('empty)
  }

  "A message from A to B in future time t+2" should "arrive to B after B advances its simulation time twice" in {
    val nodeA = newNode
    val nodeB = newNodeInTime(1)

    nodeA.sendMessage(nodeB.actor, 3, "message")

    Thread.sleep(500)
    nodeB.advanceSimulationTime()
    nodeB.advanceSimulationTime()
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
    val wrapper1: ArgumentCaptor[NodeActorWrapper] = ArgumentCaptor.forClass(classOf[NodeActorWrapper])
    val wrapper2: ArgumentCaptor[NodeActorWrapper] = ArgumentCaptor.forClass(classOf[NodeActorWrapper])
    verify(causalityErrorStrategy).handleCausalityError(
      org.mockito.ArgumentMatchers.eq(0L),
      org.mockito.ArgumentMatchers.eq(1L),
      wrapper1.capture(),
      wrapper2.capture(),
      org.mockito.ArgumentMatchers.eq("message"))
    wrapper1.getValue.actor should be(nodeB.actor)
    wrapper2.getValue.actor should be(nodeA.actor)
  }
}

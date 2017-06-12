package fr.univ_lille.cristal.emeraude.chasqui.tests

class ConnectionSpec extends ChasquiBaseSpec {

  "A new node" should "have no neighbours" in {
    val node1 = newNode
    node1.getIngoingConnections should be ('empty)
    node1.getOutgoingConnections should be ('empty)
  }

  "A connection A->B" should "create an outgoing connection in A" in {
    val nodeA = newNode
    val nodeB = newNode

    nodeA.connectTo(nodeB)

    nodeA.getOutgoingConnections should contain (nodeB.actor)
  }

  "A connection A->B" should "create an ingoing connection in B" in {
    val nodeA = newNode
    val nodeB = newNode

    nodeA.connectTo(nodeB)

    Thread.sleep(500)

    nodeB.getIngoingConnections should contain (nodeA.actor)
  }
}

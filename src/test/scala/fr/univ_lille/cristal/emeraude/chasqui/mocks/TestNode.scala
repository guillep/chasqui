package fr.univ_lille.cristal.emeraude.chasqui.mocks

import fr.univ_lille.cristal.emeraude.chasqui.core.{Messaging, Node, NodeImpl}

/**
  * Created by guille on 10/04/17.
  */
trait TestNode extends Node {
  def getReceivedMessages: Set[Any]
}

class TestNodeImpl extends NodeImpl with TestNode {
  val messages = new scala.collection.mutable.HashSet[Any]
  override def receiveMessage(message: Any, sender: Messaging): Unit = {
    messages.add(message)
  }

  def getReceivedMessages: Set[Any] = messages.toSet
}
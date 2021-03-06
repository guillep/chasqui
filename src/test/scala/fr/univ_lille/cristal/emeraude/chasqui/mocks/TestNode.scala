package fr.univ_lille.cristal.emeraude.chasqui.mocks

import akka.actor.ActorRef
import akka.pattern.ask
import akka.util.Timeout
import fr.univ_lille.cristal.emeraude.chasqui.core.NodeImpl
import fr.univ_lille.cristal.emeraude.chasqui.core.typed.TypedNode

import scala.collection.Set
import scala.concurrent.Await
import scala.concurrent.duration._

/**
  * Created by guille on 10/04/17.
  */
object TestNode {
  object GetReceivedMessages
}

class TypedTestNode(actor: ActorRef) extends TypedNode(actor) with TestNode {
  import TestNode._
  override def getReceivedMessages: Set[Any] = {
    Await.result(actor ? GetReceivedMessages, Timeout(21474835 seconds).duration).asInstanceOf[Set[Any]]
  }
}

trait TestNode {
  def getReceivedMessages: Set[Any]
}

class TestNodeImpl(automaticallyProcessQuantum: Boolean) extends NodeImpl with TestNode {
  import TestNode._

  if (!automaticallyProcessQuantum){
    this.doNotAutomaticallyProcessQuantum()
  }

  val messages = new scala.collection.mutable.HashSet[Any]
  override def internalReceiveMessage(message: Any, sender: ActorRef): Unit = {
    messages.add(message)
    super.internalReceiveMessage(message, sender)
  }

  override def receiveMessage(message: Any, sender: ActorRef): Unit = {
    //Do nothing
  }

  def getReceivedMessages: Set[Any] = messages.toSet

  override def receive = super.receive orElse {
    case GetReceivedMessages => sender ! this.getReceivedMessages
  }

}

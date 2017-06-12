package fr.univ_lille.cristal.emeraude.chasqui.tests

import akka.actor.{ActorSystem, TypedActor, TypedProps}
import fr.univ_lille.cristal.emeraude.chasqui.core.causality.IgnoreCausalityErrorStrategy
import fr.univ_lille.cristal.emeraude.chasqui.mocks.{TestNode, TestNodeImpl}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

/**
  * Created by guille on 10/04/17.
  */
class ChasquiBaseSpec extends FlatSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  var system: ActorSystem = _

  def newNodeInTime(t : Int): TestNode = {
    val node = this.newNode
    node.setTime(t)
    node.setCausalityErrorStrategy(new IgnoreCausalityErrorStrategy)
    node
  }

  def newNode: TestNode = TypedActor(system).typedActorOf(TypedProps[TestNodeImpl]())
  def newNode(name: String): TestNode = {
    val node: TestNode = TypedActor(system).typedActorOf(TypedProps[TestNodeImpl](), name=name)
    node.setId(name)
    node
  }

  override def beforeEach() = {
    system = ActorSystem.create("test")
  }

  override def afterEach() = {
    system.terminate()
    system = null
  }
}

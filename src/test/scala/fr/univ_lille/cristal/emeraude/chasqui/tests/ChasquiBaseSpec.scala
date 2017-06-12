package fr.univ_lille.cristal.emeraude.chasqui.tests

import akka.actor.{ActorSystem, Props}
import fr.univ_lille.cristal.emeraude.chasqui.core.causality.IgnoreCausalityErrorStrategy
import fr.univ_lille.cristal.emeraude.chasqui.mocks.{TestNodeImpl, TestNodeWrapper}
import org.scalatest.mockito.MockitoSugar
import org.scalatest.{BeforeAndAfterEach, FlatSpec, Matchers}

/**
  * Created by guille on 10/04/17.
  */
class ChasquiBaseSpec extends FlatSpec with Matchers with MockitoSugar with BeforeAndAfterEach {

  var system: ActorSystem = _

  def newNodeInTime(t : Int): TestNodeWrapper = {
    val node = this.newNode
    node.setTime(t)
    node.setCausalityErrorStrategy(new IgnoreCausalityErrorStrategy)
    node
  }

  def newNode: TestNodeWrapper = new TestNodeWrapper(system.actorOf(Props[TestNodeImpl]()))
  def newNode(name: String): TestNodeWrapper = {
    val node = system.actorOf(Props[TestNodeImpl](), name=name)
    val wrapper = new TestNodeWrapper(node)
    wrapper.setId(name)
    wrapper
  }

  override def beforeEach() = {
    system = ActorSystem.create("test")
  }

  override def afterEach() = {
    system.terminate()
    system = null
  }
}

package fr.univ_lille.cristal.emeraude.chasqui.tests

import akka.testkit.TestActorRef
import fr.univ_lille.cristal.emeraude.chasqui.core.NodeImpl
import fr.univ_lille.cristal.emeraude.chasqui.core.synchronization.GlobalSynchronizerWithLocalQueueSingletonActor
import fr.univ_lille.cristal.emeraude.chasqui.core.synchronization.GlobalSynchronizerWithLocalQueueStrategy.RegisterNode

/**
  * Created by guille on 10/04/17.
  */
class GlobalSynchronizerWithLocalQueueSpec extends ChasquiBaseSpec {

  "A global synchronizer" should "not advance if not all nodes are ready" in {
    val actorRef = TestActorRef[GlobalSynchronizerWithLocalQueueSingletonActor]
    val node = mock[TestActorRef[NodeImpl]]
    actorRef ! RegisterNode(node)
  }
}

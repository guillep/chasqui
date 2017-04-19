package fr.univ_lille.cristal.emeraude.chasqui.tests

import fr.univ_lille.cristal.emeraude.chasqui.core.{CausalityErrorStrategy, SynchronizerStrategy}
import org.mockito.Mockito.verify

/**
  * Created by guille on 10/04/17.
  */
class SynchronizerSpec extends ChasquiBaseSpec {

  "A node that processed all messages in time t" should "inform its synchronizer strategy to wait" in {
    val nodeA = newNode

    val synchronizerStrategy = mock[SynchronizerStrategy]
    nodeA.setSynchronizerStrategy(synchronizerStrategy)

    //This will process all message in time 1 and inform it finished to its synchronizer strategy
    nodeA.advanceSimulationTime()

    //We sleep here to wait the actor system to react
    //This is not the best way to test, because it may not scale in the future
    // but it's a practical and simple one to start with
    Thread.sleep(500)
    verify(synchronizerStrategy).notifyFinishedTime(nodeA, 1, 0)
  }
}

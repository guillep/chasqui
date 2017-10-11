package fr.univ_lille.cristal.emeraude.chasqui.tests

import fr.univ_lille.cristal.emeraude.chasqui.core.SynchronizerStrategy
import org.mockito.ArgumentMatchers
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
    nodeA.processNextQuantum()

    //We sleep here to wait the actor system to react
    //This is not the best way to test, because it may not scale in the future
    // but it's a practical and simple one to start with
    Thread.sleep(500)

    verify(synchronizerStrategy).notifyFinishedTime(
      org.mockito.ArgumentMatchers.eq(nodeA.actor), //Sender actor
      ArgumentMatchers.any(),                       //Node object
      org.mockito.ArgumentMatchers.eq(0L),          //Simulation T
      org.mockito.ArgumentMatchers.eq(0),           //Number of scheduled messages
      org.mockito.ArgumentMatchers.eq(0))           //Message delta (sent - received)

  }
}

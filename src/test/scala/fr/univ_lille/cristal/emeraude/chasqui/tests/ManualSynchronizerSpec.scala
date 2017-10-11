package fr.univ_lille.cristal.emeraude.chasqui.tests

import fr.univ_lille.cristal.emeraude.chasqui.core.synchronization.ManualSynchronizerStrategy

/**
  * Created by guille on 10/04/17.
  */
class ManualSynchronizerSpec extends ChasquiBaseSpec {

  "A node with manual synchronizer strategy" should "not advance in time" in {
    val nodeA = newNode

    nodeA.setSynchronizerStrategy(new ManualSynchronizerStrategy)

    //This will process all messages in time 1 and inform it finished to its synchronizer strategy
    nodeA.processNextQuantum()

    //Manual synchronizer strategy will not advance simulation time automatically.
    //Simulation time will stay in 0
    nodeA.getCurrentSimulationTime() should be(0)
  }
}

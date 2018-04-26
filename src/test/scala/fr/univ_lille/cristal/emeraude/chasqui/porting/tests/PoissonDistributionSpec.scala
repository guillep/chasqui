package fr.univ_lille.cristal.emeraude.chasqui.porting.tests

import fr.univ_lille.cristal.emeraude.chasqui.porting.PoissonDistribution
import fr.univ_lille.cristal.emeraude.chasqui.tests.ChasquiBaseSpec

/**
  * Created by guille on 24/04/17.
  */
class PoissonDistributionSpec extends ChasquiBaseSpec {

  "A poisson distribution average frequency" should "be ~(max spiking frequency - min spiking frequency) * exposition time" in {
    val startTime = 17
    val endTime = 350000
    val minimumSpikingFrequence = 4
    val maximumSpikingFrequence = 23
    val distribution = new PoissonDistribution(startTime, endTime, minimumSpikingFrequence, maximumSpikingFrequence)

    val valueToDistribute = 1.0

    distribution.getAverageFrequency(valueToDistribute) should
      be((minimumSpikingFrequence + valueToDistribute * (maximumSpikingFrequence-minimumSpikingFrequence)) * ((endTime - startTime).toFloat / 1e6f))
  }
}

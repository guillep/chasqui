package fr.univ_lille.cristal.emeraude.chasqui.porting

import scala.annotation.tailrec
import scala.util.Random

/**
  * Created by guille on 19/04/17.
  */
class PoissonDistribution(startTime: Long, endTime: Long, minimumSpikingFrequence: Double = 0, maximumSpikingFrequence: Double = 22) {

  type Timestamp = Long

  val expositionTime: Timestamp = endTime - startTime
  val random: Random = new Random()

  def this(expositionTime: Long) = {
    this(0, expositionTime)
  }

  def applyTo(value: Double): List[Timestamp] = {
    distribute(value)
  }

  def distribute(value: Double) = {
    innerPoisson(startTime, this.getAverageFrequency(value), List())
  }

  def getAverageFrequency(value: Double) = (minimumSpikingFrequence + value * (maximumSpikingFrequence - minimumSpikingFrequence)) * (expositionTime.toFloat / 1e6f)

  @tailrec
  private def innerPoisson(currentTime: Timestamp, averageSpike: Double, acc: List[Timestamp]): List[Timestamp] = {
    val next = getNextSampleTime(averageSpike)
    if (currentTime + next < endTime) {
      innerPoisson(currentTime + next, averageSpike, (currentTime + next) :: acc)
    }
    else {
      acc
    }
  }

  private def getNextSampleTime(averageSpike: Double) = {
    if (averageSpike != 0.0) {
      (-math.log(1.0 - random.nextDouble()) / (1.0 / (expositionTime.toDouble / averageSpike))).toLong
    } else {
      endTime + 1
    }
  }
}
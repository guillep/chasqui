package fr.univ_lille.cristal.emeraude.chasqui.porting

import scala.util.Random

/**
  * Created by guille on 15/05/17.
  */
object OutputDigitalHex extends App {

  val data = new DigitalHexInputStream().characters.sortBy(_=> Random.nextDouble ).map(_.flatten)

  data.foreach(pixels => {
      val distribution = new PoissonDistribution(0, 350000, 0, 23)
      pixels.foreach( pixel => {
        val distributedValues = distribution.applyTo(pixel)
        println(distributedValues)
      })})

}

/************************************************************************************************************
  * Contributors:
  * 		- pierre
  * 		- damien.marchal@univ-lille1.fr
  * 		- guillermo.polito@univ-lille1.fr
  ***********************************************************************************************************/
package fr.univ_lille.cristal.emeraude.chasqui.porting

import scala.math.{exp, max, min}

/********************************************************************************************************
  * Formulae to increase and decrease the weight of the synapsis.
  * The formulas are taken from: http://ieeexplore.ieee.org/stamp/stamp.jsp?tp=&arnumber=6033439
  *******************************************************************************************************/
object QBGFormula {
  def increaseWeight(g: Float, alf_p: Float, g_min: Float, g_max: Float, beta: Float) : Float = {
    val dg = (alf_p * exp(-beta * ((g - g_min) / (g_max - g_min)))).toFloat
    min(g_max, g + max(dg, Float.MinPositiveValue))
  }

  def decreaseWeight(g: Float, alf_m: Float, g_min: Float, g_max: Float, beta: Float) : Float = {
    val dg = (alf_m * exp(-beta * ((g_max - g) / (g_max - g_min)))).toFloat
    max(g_min, g - max(dg, Float.MinPositiveValue))
  }

  // positive/negative version
  def increaseWeightWithNeg(g: Float, alf_p: Float, g_min: Float, g_max: Float, beta: Float) : Float = {
    val g_norm = (g+1f)/2f
    val dg = alf_p*0.1f/* (alf_p * exp(-beta * ((g_norm - g_min) / (g_max - g_min)))).toFloat*/
    min(g_max, g_norm + max(dg, Float.MinPositiveValue))*2f-1f
  }

  def decreaseWeightWithNeg(g: Float, alf_m: Float, g_min: Float, g_max: Float, beta: Float) : Float = {
    val g_norm = (g+1f)/2f
    val dg = alf_m*0.1f/*(alf_m * exp(-beta * ((g_max - g_norm) / (g_max - g_min)))).toFloat*/
    max(g_min, g_norm - max(dg, Float.MinPositiveValue))*2f-1f
  }
}

object QBGParameters {
  var thetaLeak = 1e7 * 1e3
  var theta_step = 0.05
  var R = 1
  var v = 1

  var g_max = 1.0f
  var g_min = 0.0001f
  var alf_p = 0.06f
  var alf_m = 0.02f
  var beta_p = 2f
  var beta_m = 3f

  // Weight initialize
  var init_mean = 0.5f
  var init_std = 0.25f

  var stdpWindow = 50000
}
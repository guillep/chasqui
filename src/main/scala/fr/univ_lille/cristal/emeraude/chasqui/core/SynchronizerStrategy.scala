package fr.univ_lille.cristal.emeraude.chasqui.core

/**
  * Created by guille on 19/04/17.
  */
trait SynchronizerStrategy {
  def registerNode(self: Node): Unit
  def notifyFinishedTime(node: Node, t: Int, queueSize: Int): Unit
}

package fr.univ_lille.cristal.emeraude.chasqui.core

/**
  * Created by guille on 10/04/17.
  */
class Message(message: Any, timestamp: Int, sender: Node) {

  def getTimestamp: Int = timestamp
  def getSender: Node = sender
  def getMessage: Any = message
}

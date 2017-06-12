package fr.univ_lille.cristal.emeraude.chasqui.core

/**
  * Created by guille on 10/04/17.
  */
class Message(message: Any, timestamp: Long, sender: Messaging) {

  def getTimestamp: Long = timestamp
  def getSender: Messaging = sender
  def getMessage: Any = message
}

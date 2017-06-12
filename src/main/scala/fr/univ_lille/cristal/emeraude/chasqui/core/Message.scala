package fr.univ_lille.cristal.emeraude.chasqui.core

import akka.actor.ActorRef

/**
  * Created by guille on 10/04/17.
  */
class Message(message: Any, timestamp: Long, sender: ActorRef) {

  def getTimestamp: Long = timestamp
  def getSender: ActorRef = sender
  def getMessage: Any = message
}

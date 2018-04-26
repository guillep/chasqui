package fr.univ_lille.cristal.emeraude.chasqui.porting

import akka.actor.{ActorRef, Props}
import fr.univ_lille.cristal.emeraude.chasqui.core.NodeImpl
import fr.univ_lille.cristal.emeraude.chasqui.porting.DigitalHexInputNode.PushSpikes
import fr.univ_lille.cristal.emeraude.chasqui.porting.InputNeuron.SpikeAt

object DigitalHexInputNode {
  def newProps(charactersToTrain: Seq[Int] = 0 to 15, repetitions:Int = 1000, oversampling:Int = 1): Props = {
    Props(new DigitalHexInputNode(charactersToTrain, repetitions, oversampling))
  }

  object PushSpikes
}

class DigitalHexInputNode(charactersToTrain: Seq[Int], repetitions: Int, oversampling: Int) extends NodeImpl {
  private val expositionTime = 150000
  private val pauseTime = 350000

  protected val digitalHexInput = new DigitalHexInputStream()
  protected val numberOfCharacters = charactersToTrain.size

  protected val charactersPixelsToTrain: List[List[List[Int]]] = digitalHexInput.characters
    .zipWithIndex
    .filter(withIndex => charactersToTrain.contains(withIndex._2))
    .map(_._1)
    .map(this.oversample)

  protected def inputStream(start: Long): Stream[(Long, List[List[Int]])] = Stream.cons((start, charactersPixelsToTrain((start % numberOfCharacters).toInt)), inputStream(start + 1))
  protected def stream: Stream[(Long, List[List[Int]])] = inputStream(0)

  protected val digitalHexInputStream: Stream[List[Int]] = stream.take(repetitions * this.numberOfCharacters).map(_._2.flatten)
  protected val inputIterator: Iterator[List[Int]] = digitalHexInputStream.iterator

  def oversample(pixels: List[List[Int]]): List[List[Int]] = pixels.map( row => {
    val oversampledRow = row.flatMap(pixel => (1 to oversampling).toList.map(_ => pixel) )
    (1 to oversampling).flatMap( _ => oversampledRow ).toList
  })

  def pushSpikes(): Unit = {
    if (inputIterator.isEmpty){
      // No more input!
      return
    }
    //TODO: println(s"[$this] advancing Input simulation time to t=${this.getCurrentSimulationTime}")
    val pixels = inputIterator.next()
    //TODO: println(s"[$this] Pushing $pixels")
    val distribution = new PoissonDistribution(0, expositionTime, 0, 23)
    this.getOutgoingConnections.zip(pixels).foreach({
      case (neuron, pixel) =>
        val distributedValues = distribution.applyTo(pixel)
        distributedValues.foreach(value => {
          val spikeT = this.getCurrentSimulationTime + value
          this.sendMessage(neuron, spikeT, SpikeAt(spikeT))
        })
    })

    this.sendMessage(self, this.getCurrentSimulationTime + expositionTime + pauseTime, PushSpikes)
  }

  override def advanceSimulationTime(nextQuantum: Long): Unit = {
    super.advanceSimulationTime(nextQuantum)
  }

  override def processNextQuantum(): Unit = {
    if (this.getCurrentSimulationTime == 0){
      this.sendMessage(self, this.getCurrentSimulationTime, PushSpikes)
    }
    super.processNextQuantum()
  }

  override def scheduleMessage(message: Any, timestamp: Long, senderActorRef: ActorRef): Unit = {
    super.scheduleMessage(message, timestamp, senderActorRef)
  }

  override def start() = {
    super.start()
  }

  override def receiveMessage(message: Any, sender: ActorRef): Unit = {
    message match {
      case PushSpikes => {
        this.pushSpikes()
      }
    }
  }
}
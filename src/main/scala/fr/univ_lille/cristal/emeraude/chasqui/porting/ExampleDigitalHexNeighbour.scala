package fr.univ_lille.cristal.emeraude.chasqui.porting

/**
  * Created by guille on 19/04/17.
  */
/*object ExampleDigitalHexNeighbour extends App {

/*  QBGParameters.alf_m = 0.02f
  QBGParameters.alf_p = 0.06f
  QBGParameters.beta_m = 3f
  QBGParameters.beta_p = 2f
  QBGParameters.stdpWindow = 50000
*/

  implicit def typedNodeToSeqOfTypedNode(node: TypedNode): Seq[TypedNode] = Seq(node)

  private def fullConnection(layer1: Seq[TypedNode], layer2: Seq[TypedNode], role: String = "default"): Unit = {
    for (neuron1 <- layer1; neuron2 <- layer2 if neuron1 != neuron2){
      neuron1.blockingConnectTo(neuron2, role)
    }
  }

  val system = ActorSystem.create("digitalhex")
  implicit val executionContext = system.dispatcher
  val inputLayer = collection.mutable.ListBuffer[TypedInputNeuron]()
  val outputLayer = collection.mutable.ListBuffer[TypedNeuron]()

  val charactersToLearn = 0 to 15
  val oversampling = 3

  val numberOfPixels = (3 * oversampling) * (5 * oversampling)

  class Model extends LIFNeuronImpl with QBGSignedSynapse

  //Input node
  val input = system.actorOf(DigitalHexInputNode.newProps(charactersToLearn))
  val inputWrapper = new TypedNode(input)
  inputWrapper.setId(s"InputNode")

  //Input layer
  for(i <- 0 to numberOfPixels - 1){
    val neuron = system.actorOf(Props[InputNeuronImpl]())
    val neuronActorWrapper = new TypedInputNeuron(neuron)
    neuronActorWrapper.setId(s"InputNeuron(${(i/ (3 * oversampling)) + 1},${Math.floorMod(i , (3*oversampling))})")
    inputLayer += neuronActorWrapper
  }

  //Output layer
  for(i <- 1 to charactersToLearn.size + 4){
    val neuron = system.actorOf(Props[Model]())
    val neuronActorWrapper = new TypedNeuron(neuron)
    neuronActorWrapper.setId(s"Neuron($i)")
    neuronActorWrapper.setMembraneThresholdPotential(2 * oversampling*oversampling)
    outputLayer += neuronActorWrapper
  }

  (inputWrapper ++ inputLayer ++ outputLayer).foreach( neuron => {
    neuron.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
  })

  this.fullConnection(inputWrapper, inputLayer)
  this.fullConnection(inputLayer, outputLayer)
  this.fullConnection(outputLayer, outputLayer, LIFNeuron.inhibitRoleName)

  Thread.sleep(5000)

  inputWrapper.start()

  Thread.sleep(5000)

  (inputLayer ++ outputLayer).foreach( neuron => {
    neuron.checkPendingMessagesInQueue()
  })

  system.actorOf(Props(new SynapsesWeightGraphActor(
    outputLayer,
    outputLayer.size - 1,
    10,
    10,
    1000/24,
    null,
    3 * oversampling
  )))
}
*/
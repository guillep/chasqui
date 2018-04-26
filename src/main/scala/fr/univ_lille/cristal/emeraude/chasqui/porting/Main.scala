package fr.univ_lille.cristal.emeraude.chasqui.porting

/**
  * Created by guille on 11/10/17.
  */
/*object Main extends App {

  val system = ActorSystem.create("test")
  val input = new TypedNode(system.actorOf(Props(new NodeImpl() {

    override def advanceSimulationTime(newQuantum: Long): Unit = {
      super.advanceSimulationTime(newQuantum)
      if (newQuantum == 0) {
        (1 to 10).foreach( t => this.broadcastMessage(t, "Input" + newQuantum))
      }
      this.notifyFinishedQuantum()
    }

    override def receiveMessage(message: Any, sender: ActorRef): Unit = {
      //Nothing
    }
  })))
  val nodeA = new TypedNode(system.actorOf(Props(new NodeImpl() {
    override def receiveMessage(message: Any, sender: ActorRef): Unit = {
      val t = this.getCurrentSimulationTime()
      this.broadcastMessage(t + 1, message + "-NodeA" + t)
    }
  })))
  val nodeB = new TypedNode(system.actorOf(Props(new NodeImpl() {
    override def receiveMessage(message: Any, sender: ActorRef): Unit = {
      println(message + "-NodeB" + this.getCurrentSimulationTime())
    }
  })))

  input.blockingConnectTo(nodeA, "default")
  nodeA.blockingConnectTo(nodeB, "default")

  Thread.sleep(500)

  input.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
  input.setId("Input")
  nodeA.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
  nodeA.setId("NodeA")
  nodeB.setSynchronizerStrategy(new NeighbourSynchronizerStrategyWithLookahead())
  nodeB.setId("NodeB")

  Thread.sleep(500)

  input.start()
  nodeA.start()
  nodeB.start()
}
*/
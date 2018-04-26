package fr.univ_lille.cristal.emeraude.chasqui.porting

import java.awt._
import java.awt.event.{ActionEvent, ActionListener}
import java.util.concurrent.TimeUnit
import javax.swing.{JFrame, JPanel, SwingUtilities, Timer}

import akka.actor.Actor
import akka.util.Timeout

import scala.collection.mutable
import scala.util.{Failure, Success}

/**
  * Created by guille on 24/04/17.
  */
class SynapsesWeightGraphActor(outputs: Seq[TypedNeuron],
                               numberToIgnoreLeft: Int = 0,
                               caseWidth: Int = 8,
                               caseHeight: Int = 8,
                               refreshRate: Int = 1000 / 24,
                               viewPanel: JPanel = null,
                               rowWidth:Int) extends Actor {

  import context._
  implicit val timeout = Timeout(60, TimeUnit.SECONDS)

  private val views = mutable.Map[TypedNeuron, SynapsesWeight2DPixel]()
  var frame: Container = viewPanel

  SwingUtilities.invokeAndWait(() => {
    if (frame == null) {
      frame = new JFrame()
      frame.setLayout(new FlowLayout)
    }

    outputs.foreach ( neuron => {
      val aSynapseWeight2DPixel = new SynapsesWeight2DPixel(rowWidth, caseWidth, caseHeight)
      views.+= (neuron -> aSynapseWeight2DPixel)
      frame.add(aSynapseWeight2DPixel)
    })

    frame.setVisible(true)
    frame.setSize(new Dimension(800, 1000))
  }
  )

  private val delay = refreshRate / outputs.size
  private var currentN = 0
  private val taskPerformer = new ActionListener() {
    override def actionPerformed(actionEvent:  ActionEvent): Unit = {
      update(currentN)
      currentN = (currentN + 1) % outputs.size
    }
  }
  val timer = new Timer(delay, taskPerformer)
  timer.start()

  def update(n: Int): Unit = {
    views.toSeq(n) match {
      case (path, view) =>
        val weightsF = path.getIngoingConnectionWeights()
        weightsF onComplete {
          case Success(weights) => {
            view.updateValue(weights.take(weights.size - numberToIgnoreLeft))
            SwingUtilities.invokeLater(() => {
              view.repaint()
              frame.revalidate()
            })
          }
          case Failure(t) => println("Error while fetching neuron synapses")
        }
      case other => throw new Exception("Unknown " + other)
    }
  }

  def receive = {
    case m => println("[SynapsesWeightGraphActor] Unknown message " + m)
  }
}

class SynapsesWeight2DPixel(layerWidth: Int = 0, caseWidth: Int = 1, caseHeight: Int = 1) extends JPanel {
  var hasValidValues = false
  var isInited = false
  private var colorMap = Array.ofDim[Int](1, 1)

  def updateValue(values: Seq[Double]) : Unit = {
    hasValidValues = true
    val width = math.min(layerWidth, values.size)
    val height = Math.ceil(values.size.asInstanceOf[Float] / layerWidth).asInstanceOf[Int]
    colorMap = Array.ofDim[Int](width, height)

    val min_v = if (values.isEmpty) 0 else values.min
    val max_v = if (values.isEmpty) 1.0 else values.max

    values.zipWithIndex.foreach { case (value, index) =>
      val color = if (value < 0) {
        val v = ((value / math.min(-1.0f, min_v)) * 255f).toInt
        (v << 16) + (v << 8) + v
      }
      else {
        val v = value / math.max(1.0f, max_v)
        val rc = (math.max(0f, -2f * v * v + 4f * v - 1f) * 255f).toInt
        val bc = (math.max(0f, -2f * v * v + 1) * 255f).toInt
        val gc = (math.max(0f, 4f * v * (-v + 1f)) * 255f).toInt
        (rc << 16) + (gc << 8) + bc
      }
      colorMap(index % layerWidth)(index / layerWidth) = color
    }

    if (!isInited) {
      val d = new Dimension(width * caseWidth, height * caseHeight)
      setPreferredSize(d)
      setSize(d)
      isInited = true
    }
  }

  override def paintComponent(g: Graphics): Unit = {
    if (!hasValidValues) {
      return
    }

    for ((line, x) <- colorMap.zipWithIndex) {
      for ((v, y) <- line.zipWithIndex) {
        g.setColor(new Color(v))
        g.fillRect(x * caseWidth, y * caseHeight, caseWidth, caseHeight)
      }
    }
  }

  def destroy(): Unit = {

  }
}


package fr.univ_lille.cristal.emeraude.chasqui.porting

import java.awt.event.{ActionEvent, WindowAdapter, WindowEvent}
import java.awt.{Component, Dimension, FlowLayout}
import javax.swing._
import javax.swing.event.ListSelectionEvent
import javax.swing.table.AbstractTableModel

import fr.univ_lille.cristal.emeraude.chasqui.core.typed.TypedNode

import scala.collection.mutable

class NodeInspector(nodes: Seq[TypedNode]) {

  val buttonSpecs = new mutable.ListBuffer[(String, Action)]()
  var nodeTable: JTable = null

  def addButton(text: String, action: Action) = {
    this.buttonSpecs += (text -> action)
    this
  }

  def buildButtonFromSpec(spec: (String, Action)) = {
    val button = new JButton()
    button.setPreferredSize(new Dimension(150, 50))
    button.setVerticalTextPosition(SwingConstants.CENTER);
    button.setHorizontalTextPosition(SwingConstants.LEADING); //aka LEFT, for left-to-right locales
    button.setAction(spec._2)
    //Set tooltip and text after action.
    //Otherwise it is not shown
    button.setText(spec._1)
    button
  }

  def exit() = {
    System.exit(0)
  }

  var idValue: JLabel = null
  var messageQueueValue: JList[String] = null
  var quantumValue: JLabel = null
  var statusValue: JLabel = null
  var messageDeltaValue: JLabel = null

  def open() = {
    val frame = new JFrame()
    frame.setLayout(new BoxLayout(frame.getContentPane, BoxLayout.Y_AXIS))

    val idLabel = new JLabel("Id")
    idValue = new JLabel()
    val idRowPanel = new JPanel()
    idRowPanel.setLayout(new BoxLayout(idRowPanel, BoxLayout.X_AXIS))
    idRowPanel.add(idLabel)
    idRowPanel.add(idValue)

    val quantumLabel = new JLabel("t")
    quantumLabel.setPreferredSize(new Dimension(50, 500))
    quantumValue = new JLabel()
    quantumValue.setPreferredSize(quantumLabel.getPreferredSize)

    val statusLabel = new JLabel("Status")
    statusValue = new JLabel()

    val messageQueueLabel = new JLabel("MessageQueue")
    messageQueueLabel.setPreferredSize(new Dimension(500, 500))
    messageQueueValue = new JList[String]()

    val messageQueueScroller = new JScrollPane(messageQueueValue)
    messageQueueScroller .setPreferredSize(messageQueueLabel.getPreferredSize)

    val messageDeltaLabel = new JLabel("MessageDelta")
    messageDeltaValue = new JLabel()

    val labelPanel = new JPanel()
    labelPanel.setLayout(new BoxLayout(labelPanel, BoxLayout.Y_AXIS))
    labelPanel.add(idLabel)
    labelPanel.add(quantumLabel)
    labelPanel.add(statusLabel)
    labelPanel.add(messageQueueLabel)
    labelPanel.add(messageDeltaLabel)

    val valuePanel = new JPanel()
    valuePanel.setLayout(new BoxLayout(valuePanel, BoxLayout.Y_AXIS))
    labelPanel.add(idValue)
    valuePanel.add(quantumValue)
    valuePanel.add(statusValue)
    valuePanel.add(messageQueueScroller)
    valuePanel.add(messageDeltaValue)

    val nodeInfoPanel = new JPanel()
    nodeInfoPanel.setLayout(new BoxLayout(nodeInfoPanel, BoxLayout.X_AXIS))
    nodeInfoPanel.setAlignmentY(Component.TOP_ALIGNMENT)
    nodeInfoPanel.add(labelPanel)
    nodeInfoPanel.add(valuePanel)
    nodeInfoPanel.setPreferredSize(new Dimension(500, 1000))

    val mainPane = new JPanel()
    mainPane.setLayout(new BoxLayout(mainPane, BoxLayout.X_AXIS))
    mainPane.add(this.buildNodeTable)
    mainPane.add(nodeInfoPanel)

    val buttonPane = new JPanel()
    buttonPane.setLayout(new FlowLayout)
    buttonSpecs.foreach( s => {
      buttonPane.add(this.buildButtonFromSpec(s))
    })
    buttonPane.add(this.buildRefreshButton)

    frame.add(mainPane)
    frame.add(buttonPane)

    frame.pack()
    frame.setVisible(true)

    frame.addWindowListener(new WindowAdapter {
      override def windowClosing(e: WindowEvent): Unit = {
        NodeInspector.this.exit()
      }
    })
  }

  // Updates the UI
  def selectedNode(node: TypedNode): Unit = {
    val summary = node.getNodeSummary()
    idValue.setText(summary._1.toString)
    statusValue.setText(summary._2.toString)
    quantumValue.setText(summary._3.toString)
    messageQueueValue.setListData(summary._4.map(m => m.toString).toArray)
    messageDeltaValue.setText(summary._5.toString)
  }

  private def buildNodeTable = {
    val listModel = new DefaultListModel[TypedNode]()
    nodes.foreach(n => listModel.addElement(n))

    nodeTable = new JTable(new NodeListTableModel(nodes))
    nodeTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION)

    nodeTable.getSelectionModel.addListSelectionListener((e: ListSelectionEvent) => {
      val lsm = e.getSource.asInstanceOf[ListSelectionModel]
      if (!lsm.isSelectionEmpty()){
        NodeInspector.this.selectedNode(nodes(lsm.getMinSelectionIndex))
      }
    })

    val listScroller = new JScrollPane(nodeTable)
    listScroller.setPreferredSize(new Dimension(200, 200))
    listScroller
  }

  private def buildRefreshButton = {
    val button = new JButton()
    button.setPreferredSize(new Dimension(150, 50))
    button.setVerticalTextPosition(SwingConstants.CENTER);
    button.setHorizontalTextPosition(SwingConstants.LEADING); //aka LEFT, for left-to-right locales
    button.setAction(new AbstractAction() {
      override def actionPerformed(e: ActionEvent): Unit = {
        NodeInspector.this.nodeTable.invalidate()
      }
    })
    //Set tooltip and text after action.
    //Otherwise it is not shown
    button.setText("Refresh")
    button.setToolTipText("Refresh")
    button
  }
}

class NodeTableModel(nodes: Seq[TypedNode]) extends AbstractTableModel {

  val columns = Seq("name")

  override def getValueAt(rowIndex: Int, columnIndex: Int): AnyRef = {
    if (columnIndex == 0){
      return nodes(rowIndex).getId().toString
    }
    return "Unknown Column"
  }

  override def getRowCount: Int = nodes.size

  override def getColumnCount: Int = columns.size

  override def getColumnName(column: Int): String = {
    columns(column)
  }
}

class NodeListTableModel(nodes: Seq[TypedNode]) extends AbstractTableModel {

  val columns = Seq("name", "t")

  override def getValueAt(rowIndex: Int, columnIndex: Int): AnyRef = {
    if (columnIndex == 0){
      return nodes(rowIndex).getId().toString
    }
    if (columnIndex == 1){
      return nodes(rowIndex).getCurrentSimulationTime().toString
    }
    return "Unknown Column"
  }

  override def getRowCount: Int = nodes.size

  override def getColumnCount: Int = columns.size

  override def getColumnName(column: Int): String = {
    columns(column)
  }
}
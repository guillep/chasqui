package fr.univ_lille.cristal.emeraude.chasqui.porting


class DigitalHexInputStream {

  val characters = List(
    List(
      List(1, 1, 1),
      List(1, 0, 1),
      List(1, 0, 1),
      List(1, 0, 1),
      List(1, 1, 1)
    ),
    List(
      List(0, 0, 1),
      List(0, 0, 1),
      List(0, 0, 1),
      List(0, 0, 1),
      List(0, 0, 1)
    ),
    List(
      List(1, 1, 1),
      List(0, 0, 1),
      List(1, 1, 1),
      List(1, 0, 0),
      List(1, 1, 1)
    ),
    List(
      List(1, 1, 1),
      List(0, 0, 1),
      List(1, 1, 1),
      List(0, 0, 1),
      List(1, 1, 1)
    ),
    List(
      List(1, 0, 1),
      List(1, 0, 1),
      List(1, 1, 1),
      List(0, 0, 1),
      List(0, 0, 1)
    ),
    List(
      List(1, 1, 1),
      List(1, 0, 0),
      List(1, 1, 1),
      List(0, 0, 1),
      List(1, 1, 1)
    ),
    List(
      List(1, 1, 1),
      List(1, 0, 0),
      List(1, 1, 1),
      List(1, 0, 1),
      List(1, 1, 1)
    ),
    List(
      List(1, 1, 1),
      List(0, 0, 1),
      List(0, 0, 1),
      List(0, 0, 1),
      List(0, 0, 1)
    ),
    List(
      List(1, 1, 1),
      List(1, 0, 1),
      List(1, 1, 1),
      List(1, 0, 1),
      List(1, 1, 1)
    ),
    List(
      List(1, 1, 1),
      List(1, 0, 1),
      List(1, 1, 1),
      List(0, 0, 1),
      List(1, 1, 1)
    ),
    List(
      List(1, 1, 1),
      List(1, 0, 1),
      List(1, 1, 1),
      List(1, 0, 1),
      List(1, 0, 1)
    ),
    List(
      List(1, 0, 0),
      List(1, 0, 0),
      List(1, 1, 1),
      List(1, 0, 1),
      List(1, 1, 1)
    ),
    List(
      List(1, 1, 1),
      List(1, 0, 0),
      List(1, 0, 0),
      List(1, 0, 0),
      List(1, 1, 1)
    ),
    List(
      List(0, 0, 1),
      List(0, 0, 1),
      List(1, 1, 1),
      List(1, 0, 1),
      List(1, 1, 1)
    ),
    List(
      List(1, 1, 1),
      List(1, 0, 0),
      List(1, 1, 0),
      List(1, 0, 0),
      List(1, 1, 1)
    ),
    List(
      List(1, 1, 1),
      List(1, 0, 0),
      List(1, 1, 0),
      List(1, 0, 0),
      List(1, 0, 0)
    )
  )

  val label = List("0", "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F")

  var cursor = 0

  def atEnd() = cursor >= characters.size

  def next(): Seq[Float] = {
    val next = for {
      i <- 0 until 3
      j <- 0 until 5
    } yield characters(cursor)(j)(i).toFloat

    cursor += 1
    next
  }

  def reset() = {
    cursor = 0
  }

  override def toString = "[DigitHax] "+cursor+" / "+characters.size
}

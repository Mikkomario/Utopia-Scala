package utopia.genesis.graphics

import utopia.flow.operator.ordering.SelfComparable
import utopia.genesis.graphics.DrawLevel2.Normal

import scala.language.implicitConversions

object DrawOrder
{
	// ATTRIBUTES   -------------------
	
	/**
	  * The default draw order (normal level, index 0)
	  */
	val default = apply()
	
	
	// IMPLICIT -----------------------
	
	implicit def apply(level: DrawLevel2): DrawOrder = new DrawOrder(level)
}

/**
  * Specifies a relative drawing order.
  * The elements in the background are drawn first, while elements in the foreground are drawn last.
  * @author Mikko Hilpinen
  * @since 06/02/2024, v4.0
  * @param level The overall draw level / high-level category
  * @param orderIndex More precise draw-order index within 'level'.
  *                   Larger indexes are drawn after (i.e. above) lower indexes.
  */
case class DrawOrder(level: DrawLevel2 = Normal, orderIndex: Int = 0) extends SelfComparable[DrawOrder]
{
	override def self = this
	
	override def compareTo(o: DrawOrder) = {
		val levelCompare = level.compareTo(o.level)
		if (levelCompare == 0)
			orderIndex - o.orderIndex
		else
			levelCompare
	}
}

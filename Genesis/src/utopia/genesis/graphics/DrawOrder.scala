package utopia.genesis.graphics

import utopia.flow.operator.combine.{Combinable, Subtractable}
import utopia.flow.operator.ordering.SelfComparable
import utopia.genesis.graphics.DrawLevel.Normal

import scala.language.implicitConversions

object DrawOrder
{
	// ATTRIBUTES   -------------------
	
	/**
	  * The default draw order (normal level, index 0)
	  */
	val default = apply()
	
	
	// IMPLICIT -----------------------
	
	implicit def apply(level: DrawLevel): DrawOrder = new DrawOrder(level)
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
case class DrawOrder(level: DrawLevel = Normal, orderIndex: Int = 0)
	extends SelfComparable[DrawOrder] with Combinable[Int, DrawOrder] with Subtractable[Int, DrawOrder]
{
	// IMPLEMENTED  -------------------
	
	override def self = this
	
	override def compareTo(o: DrawOrder) = {
		val levelCompare = level.compareTo(o.level)
		if (levelCompare == 0)
			orderIndex - o.orderIndex
		else
			levelCompare
	}
	
	override def +(other: Int): DrawOrder = copy(orderIndex = orderIndex + other)
	override def -(other: Int): DrawOrder = this + (-other)
}

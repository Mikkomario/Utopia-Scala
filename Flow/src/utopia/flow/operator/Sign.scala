package utopia.flow.operator

import utopia.flow.collection.immutable.Pair

/**
  * An enumeration for sign (positive or negative), which can also be used as binary direction enumeration
  * @author Mikko Hilpinen
  * @since 21.9.2021, v1.12
  */
sealed trait Sign extends SelfComparable[Sign] with Reversible[Sign]
{
	// ABSTRACT	-----------------------------
	
	/**
	  * @return Whether this direction is considered positive
	  */
	def isPositive: Boolean
	
	/**
	  * @return A modified applied to double numbers that have this direction (-1 | 1)
	  */
	def modifier: Short
	
	/**
	  * @return Direction opposite to this one
	  */
	def opposite: Sign
	
	
	// IMPLEMENTED  ------------------------
	
	override def unary_- = opposite
	
	
	// OTHER	----------------------------
	
	/**
	  * @param i An integer
	  * @return 'i' length to this direction
	  */
	def *(i: Int) = if (isPositive) i else -i
	/**
	  * @param d A double
	  * @return 'd' length to this direction
	  */
	def *(d: Double) = if (isPositive) d else -d
	/**
	  * @param r a reversible instance
	  * @tparam R2 Repr of that instance
	  * @tparam R instance type
	  * @return 'r' if this is positive, -r otherwise
	  */
	def *[R2, R <: Reversible[R2]](r: R) = if (isPositive) r.repr else -r
}

object Sign
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * All 2 sign values (first positive, then negative)
	  */
	val values = Pair(Positive, Negative)
	
	
	// OTHER    --------------------------
	
	/**
	  * @param item An item
	  * @return Sign of that item. Positive in case of zero.
	  */
	@deprecated("Please use item.sign instead", "v2.0")
	def of(item: SignedOrZero[_]): Sign = if (item.isPositiveOrZero) Positive else Negative
	
	/**
	  * @param number A number
	  * @return Sign of that number
	  */
	def of(number: Double): Sign = if (number >= 0.0) Positive else Negative
	
	/**
	  * @param positiveCondition A condition for returning Positive
	  * @return Positive if condition was true, Negative otherwise
	  */
	def apply(positiveCondition: Boolean) = if (positiveCondition) Positive else Negative
	
	
	// NESTED   --------------------------
	
	/**
	  * Positive sign (+). AKA positive direction (usually right / down / clockwise)
	  */
	case object Positive extends Sign
	{
		override def isPositive = true
		override def modifier = 1
		
		override def opposite = Negative
		override def repr = this
		
		override def compareTo(o: Sign) = o match {
			case Positive => 0
			case Negative => 1
		}
	}
	
	/**
	  * Negative sign (-). AKA negative direction (usually left / up / counterclockwise)
	  */
	case object Negative extends Sign
	{
		override def isPositive = false
		override def modifier = -1
		
		override def opposite = Positive
		override def repr = this
		
		override def compareTo(o: Sign) = o match {
			case Positive => -1
			case Negative => 0
		}
	}
}

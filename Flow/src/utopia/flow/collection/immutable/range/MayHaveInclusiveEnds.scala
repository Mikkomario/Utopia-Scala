package utopia.flow.collection.immutable.range

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.End
import utopia.flow.operator.enumeration.End.{First, Last}


/**
  * A common trait for items which have 0-2 ends. Both of these are inclusive.
  * @author Mikko Hilpinen
  * @since 15.8.2025, v2.7
  */
trait MayHaveInclusiveEnds[+P] extends MayHaveEnds[P]
{
	// COMPUTED -----------------------
	
	/**
	 * @return The start and the end of this range. Contains None if that side is undefined / open.
	 */
	def endOptions = Pair(startOption, endOption)
	
	/**
	  * @return Whether this span is of length 1 (i.e. the start and the end overlap)
	  */
	def isUnit = startOption.exists(endOption.contains)
	/**
	  * @return The only value contained within this range.
	  *         None if this range contains multiple values.
	  */
	def only = startOption.filter(endOption.contains)
	
	
	// IMPLEMENTED  --------------------
	
	override def isInclusive = true
	override def isEmpty = false
	
	
	// OTHER    ----------------------
	
	/**
	  * @param end The targeted end of this span
	  * @return The item at the targeted end. None if this range doesn't specify that end.
	  */
	def get(end: End) = end match {
		case First => startOption
		case Last => endOption
	}
}

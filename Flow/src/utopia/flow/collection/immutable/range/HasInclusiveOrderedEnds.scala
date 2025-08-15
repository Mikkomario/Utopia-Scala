package utopia.flow.collection.immutable.range

import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.enumeration.Extreme
import utopia.flow.operator.enumeration.Extreme.{Max, Min}

/**
  * A common trait for items which, like ranges, have two ends: A start and an end. Both of these are inclusive.
  * @author Mikko Hilpinen
  * @since 16.12.2022, v2.0
  */
trait HasInclusiveOrderedEnds[P] extends HasOrderedEnds[P] with HasInclusiveEnds[P] with MayHaveInclusiveOrderedEnds[P]
{
	// COMPUTED -----------------------
	
	/**
	  * @return The minimum and the maximum of this span
	  */
	def minMax: Pair[P] = ends.sorted
	
	
	// IMPLEMENTED  --------------------
	
	override def min = ordering.min(start, end)
	override def minOption = Some(min)
	
	override def max = ordering.max(start, end)
	override def maxOption = Some(max)
	
	
	// OTHER    ----------------------
	
	/**
	  * @param extreme Targeted extreme (min or max)
	  * @return The most extreme value at the specified end
	  */
	def apply(extreme: Extreme) = extreme match {
		case Max => max
		case Min => min
	}
}

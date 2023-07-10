package utopia.flow.operator

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.mutable.iterator.OptionsIterator
import utopia.flow.operator.Extreme.{Max, Min}
import utopia.flow.operator.Sign.{Negative, Positive}

/**
  * Common trait for items that may specify a value in either direction within increments of a specific length.
  * @author Mikko Hilpinen
  * @since 9.7.2023, v2.2
  */
trait Steppable[+Repr <: Steppable[Repr]]
{
	// ABSTRACT -----------------------
	
	/**
	  * @return This item
	  */
	def self: Repr
	/**
	  * @param direction Direction of travel (positive or negative)
	  * @return The next copy of this item towards that direction (i.e. the next larger or smaller item).
	  *         May return this item in case this is the most extreme value.
	  */
	def next(direction: Sign): Repr
	/**
	  * @param extreme Targeted extreme (min or max)
	  * @return Whether this value is the most extreme value available in the specified direction.
	  */
	def is(extreme: Extreme): Boolean
	
	
	// COMPUTED ----------------------
	
	/**
	  * @return Whether this is the largest available value
	  */
	def isMax = is(Max)
	/**
	  * @return Whether this is the smallest available value
	  */
	def isMin = is(Min)
	
	/**
	  * @return The next larger copy of this item.
	  *         May return this item if this is the largest value available.
	  */
	def more = next(Positive)
	/**
	  * @return The next smaller copy of this item.
	  *         May return this item if this is the smallest value available.
	  */
	def less = next(Negative)
	
	/**
	  * @return The next larger copy of this item. None if this is the largest value.
	  */
	def moreOption = nextOption(Positive)
	/**
	  * @return The next smaller copy of this item. None if this is the smallest value.
	  */
	def lessOption = nextOption(Negative)
	
	/**
	  * @return An iterator that returns continually larger and larger values,
	  *         until it reaches the maximum (not guaranteed)
	  */
	def moreIterator = stepper(Positive)
	/**
	  * @return An iterator that returns continually smaller and smaller values,
	  *         until it reaches the minimum (not guaranteed)
	  */
	def lessIterator = stepper(Negative)
	/**
	  * @return An iterator that returns this item, and all larger items
	  */
	def andLarger = self +: moreIterator
	/**
	  * @return An iterator that returns this item, and all smaller items
	  */
	def andSmaller = self +: lessIterator
	
	
	// OTHER    ---------------------
	
	/**
	  * @param direction Targeted direction (positive or negative)
	  * @return The next larger or smaller copy of this item, depending on the specified direction.
	  *         None if this is the most extreme value in that direction.
	  */
	def nextOption(direction: Sign) = {
		if (is(Extreme(direction)))
			None
		else
			Some(next(direction))
	}
	
	/**
	  * @param direction Targeted direction
	  * @return An iterator that continually returns more and more extreme values towards that direction,
	  *         until reaching the most extreme value (which it is not guaranteed to do)
	  */
	def stepper(direction: Sign) =
		OptionsIterator.iterate(nextOption(direction)) { _.nextOption(direction) }
}

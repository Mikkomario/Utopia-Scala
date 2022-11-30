package utopia.flow.util

import utopia.flow.operator.MaybeEmpty.HasIsEmpty

import scala.language.reflectiveCalls

/**
  * An utility object for dealing with items which may be empty
  * @author Mikko Hilpinen
  * @since 30.11.2022, v2.0
  */
object NotEmpty
{
	/**
	  * @param item An item which may be empty
	  * @tparam A Type of that item
	  * @return Some if that item is not empty. None otherwise.
	  */
	def apply[A <: HasIsEmpty](item: A) = if (item.isEmpty) None else Some(item)
	
	/**
	  * Finds the first non-empty item from the specified set of items
	  * @param items A set of items
	  * @tparam A Type of those items
	  * @return The first encountered non-empty item
	  */
	def first[A <: HasIsEmpty](items: IterableOnce[A]) = items.iterator.find { !_.isEmpty }
}

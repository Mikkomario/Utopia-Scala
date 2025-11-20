package utopia.vault.nosql.template

import utopia.vault.sql.Condition

/**
  * Common trait for interfaces which may be filtered using SQL conditions
  * @tparam Repr Type of the (filtered) copies of this view
  * @author Mikko Hilpinen
  * @since 20.11.2025, v2.1
  */
trait Filterable[+Repr]
{
	// ABSTRACT	----------------------
	
	/**
	  * @return This view
	  */
	def self: Repr
	
	/**
	 * Applies an additional filter to this view
	 * @param condition An additional filter condition applied
	 * @return A copy of this view filtered to only include item which fulfill the specified condition
	 */
	def filter(condition: Condition): Repr
	
	
	// OTHER    --------------------
	
	/**
	  * @param condition An additional filter condition to apply. None if no additional condition should be applied.
	  * @return A filtered copy of this view. This view, if 'condition' was None.
	  */
	def filter(condition: Option[Condition]): Repr = condition match {
		case Some(c) => filter(c)
		case None => self
	}
}

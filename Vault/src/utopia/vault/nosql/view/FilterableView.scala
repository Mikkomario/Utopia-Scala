package utopia.vault.nosql.view

import utopia.vault.sql.Condition

/**
  * A common trait for access points whose conditions may be enhanced to cover a more precise area
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.8
  */
trait FilterableView[+Sub] extends View
{
	// ABSTRACT	----------------------
	
	/**
	  * @return This view
	  */
	protected def self: Sub
	
	/**
	  * Applies a filter over this access point
	  * @param additionalCondition An additional search condition applied
	  * @return A copy of this access point with specified search condition in place
	  */
	def filter(additionalCondition: Condition): Sub
	
	
	// OTHER    --------------------
	
	/**
	  * @param additionalCondition An additional search condition to apply
	  *                            (None if no additional condition should be applied)
	  * @return A filtered copy of this access point.
	  *         I.e. an access point that applies the specified condition to all queries.
	  */
	def filter(additionalCondition: Option[Condition]): Sub = additionalCondition match {
		case Some(c) => filter(c)
		case None => self
	}
}

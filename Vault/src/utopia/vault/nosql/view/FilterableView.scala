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
	  * Applies a filter over this access point
	  * @param additionalCondition An additional search condition applied
	  * @return A copy of this access point with specified search condition in place
	  */
	def filter(additionalCondition: Condition): Sub
}

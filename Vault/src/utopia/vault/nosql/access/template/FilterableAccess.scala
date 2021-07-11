package utopia.vault.nosql.access.template

import utopia.vault.sql.Condition

/**
  * A common trait for access points whose conditions may be enhanced to cover a more precise area
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  */
trait FilterableAccess[+A, +Repr] extends Access[A]
{
	// ABSTRACT	----------------------
	
	/**
	  * Applies a filter over this access point
	  * @param additionalCondition An additional search condition applied
	  * @return A copy of this access point with specified search condition in place
	  */
	def filter(additionalCondition: Condition): Repr
}

package utopia.vault.nosql.factory

import utopia.vault.sql.Condition

/**
 * A common trait for factories that deal with deprecating items (deprecating, in this case means that the rows
 * should rarely be included in basic searches)
 * @author Mikko Hilpinen
 * @since 11.1.2020, v1.4
 */
trait Deprecatable
{
	// ABSTRACT	---------------------
	
	/**
	 * @return A condition that determines whether a row is deprecated
	 */
	def nonDeprecatedCondition: Condition
}

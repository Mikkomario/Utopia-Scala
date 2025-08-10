package utopia.vault.nosql.view

import utopia.vault.database.Connection
import utopia.vault.model.template.Deprecates

/**
 * Common trait for views which distinguish between active and deprecated items
 *
 * @author Mikko Hilpinen
 * @since 09.07.2025, v1.22
 */
trait DeprecatableView[+Repr] extends FilterableView[Repr]
{
	// ABSTRACT -----------------------
	
	/**
	 * @return Model used for interacting with the DB and for building conditions
	 */
	protected def model: Deprecates
	
	/**
	 * Deprecates the accessible items
	 * @param connection Implicit DB connection
	 * @return Whether any row was targeted
	 */
	def deprecate()(implicit connection: Connection): Boolean
	
	
	// COMPUTED ----------------------
	
	/**
	 * @return Access to non-deprecated items
	 */
	def active = filter(model.activeCondition)
	/**
	 * @return Access limited to deprecated items
	 */
	def historical = filter(model.deprecatedCondition)
}

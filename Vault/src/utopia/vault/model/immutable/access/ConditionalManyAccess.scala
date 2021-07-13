package utopia.vault.model.immutable.access

import utopia.vault.database.Connection
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.sql.Condition

/**
 * Provides access to possibly multiple items based on a search condition
 * @author Mikko Hilpinen
 * @since 6.10.2019, v1.3.1+
 */
@deprecated("Replaced with utopia.vault.nosql.access.many.model.ManyModelAccess", "v1.4")
trait ConditionalManyAccess[+A] extends ConditionalAccess[A]
{
	/**
	 * Reads items from database
	 * @param connection DB Connection
	 * @return all items accessed from this sub group
	 */
	def get(implicit connection: Connection) = factory.getMany(condition)
	
	/**
	 * Finds certain items based on an additional search condition
	 * @param additionalCondition An additional search condition
	 * @param connection DB Connection
	 * @return Items that fulfill both current and additional search conditions
	 */
	def find(additionalCondition: Condition)(implicit connection: Connection) = factory.getMany(
		condition && additionalCondition)
	
	/**
	 * Provides access to items under an additional search condition
	 * @param additionalCondition An additional search condition
	 * @return Access to items that fulfill both current and additional search conditions
	 */
	def subGroup(additionalCondition: Condition) = ConditionalManyAccess[A](
		condition && additionalCondition, factory)
}

object ConditionalManyAccess
{
	// OTHER	-------------------------
	
	/**
	 * Creates a new conditional access instance
	 * @param condition A condition applied to all searches
	 * @param factory Factory used for instantiating objects
	 * @tparam A Type of objects returned
	 * @return A conditional access instance
	 */
	def apply[A](condition: Condition, factory: FromResultFactory[A]): ConditionalManyAccess[A] =
		new Wrapper(condition, factory)
	
	
	// NESTED	-------------------------
	
	private class Wrapper[A](override val condition: Condition, override val factory: FromResultFactory[A])
		extends ConditionalManyAccess[A]
}

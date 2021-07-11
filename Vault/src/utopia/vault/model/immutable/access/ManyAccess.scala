package utopia.vault.model.immutable.access

import utopia.vault.sql.SqlExtensions._
import utopia.vault.database.Connection
import utopia.vault.sql.{Condition, ConditionElement}

/**
 * Used for accessing multiple instances or ids of one or more tables
 * @author Mikko Hilpinen
 * @since 30.7.2019, v1.3+
 * @tparam I Type of used index
 * @tparam A Type of read model
 */
@deprecated("Replaced with utopia.vault.nosql.access.many.model.ManyModelAccess", "v1.4")
trait ManyAccess[-I, +A] extends Access[I, A]
{
	// COMPUTED	-----------------------
	
	/**
	 * @param connection Database connection (implicit)
	 * @return All accessible items
	 */
	def all(implicit connection: Connection) = factory.getAll()
	
	
	// OTHER	-----------------------
	
	/**
	 * Finds multiple items based on a condition
	 * @param condition A condition
	 * @param connection Database connection (implicit)
	 * @return Read item(s)
	 */
	protected def find(condition: Condition)(implicit connection: Connection) = factory.getMany(condition)
	
	/**
	 * Provides access into a limited group of items based on a search condition
	 * @param condition A search condition
	 * @return Access to items within that search condition
	 */
	protected def subGroup(condition: Condition) = ConditionalManyAccess[A](condition, factory)
	
	/**
	 * Finds models for multiple ids
	 * @param ids Searched ids
	 * @param connection Database connection (implicit)
	 * @tparam I2 Type of searched id
	 * @return Models for searched ids
	 */
	def withIds[I2 <: I](ids: Set[I2])(implicit connection: Connection) =
	{
		if (ids.isEmpty)
			Vector()
		else
			factory.getMany(index in ids.map { idValue(_): ConditionElement })
	}
}

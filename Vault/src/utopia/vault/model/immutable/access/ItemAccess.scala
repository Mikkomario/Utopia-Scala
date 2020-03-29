package utopia.vault.model.immutable.access

import utopia.flow.datastructure.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.sql.{Delete, Where}

/**
 * Provides access to a single possible id / row in a database
 * @author Mikko Hilpinen
 * @since 30.7.2019, v1.3+
 * @tparam A Type of model read from table
 * @param value Value of this id
 * @param factory Factory used for reading model data from table
 */
@deprecated("Replaced with utopia.vault.nosql.access.SingleIdModelAccess", "v1.4")
class ItemAccess[+A](value: Value, override val factory: FromResultFactory[A]) extends ConditionalSingleAccess[A]
{
	// ATTRIBUTES	-------------------
	
	override val condition = factory.table.primaryColumn.get <=> value
}

package utopia.vault.nosql.access.single.model.distinct

import utopia.flow.datastructure.immutable.Value
import utopia.vault.nosql.factory.FromResultFactory

/**
 * Used for accessing a single model that has a specific id
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
class SingleIdModelAccess[+A](val id: Value, val factory: FromResultFactory[A])
	extends UniqueModelAccess[A]
{
	override def condition = table.primaryColumn.get <=> id
}
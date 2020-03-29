package utopia.vault.nosql.access

import utopia.flow.datastructure.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.nosql.factory.FromResultFactory
import utopia.vault.sql.{Condition, OrderBy}

/**
 * Used for accessing a single model that has a specific id
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
class SingleIdModelAccess[+A](val id: Value, val factory: FromResultFactory[A]) extends UniqueAccess[A]
{
	override def condition = table.primaryColumn.get <=> id
	
	override def table = factory.table
	
	override def target = factory.target
	
	override protected def read(condition: Option[Condition], order: Option[OrderBy])(
		implicit connection: Connection) = factory.get(condition.getOrElse(this.condition), order)
}
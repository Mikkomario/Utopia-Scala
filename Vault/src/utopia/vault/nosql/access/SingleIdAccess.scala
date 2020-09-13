package utopia.vault.nosql.access

import utopia.flow.datastructure.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.sql.{Condition, Limit, OrderBy, Select, Where}

/**
 * Used for accessing individual ids in a table
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait SingleIdAccess[+ID] extends IdAccess[ID, Option[ID]] with SingleAccess[ID, SingleIdAccess[ID]]
{
	// COMPUTED	-------------------------------
	
	/**
	  * @param connection Database connection (implicit)
	  * @return The smallest available row id
	  */
	def min(implicit connection: Connection) = first(OrderBy.ascending(index))
	
	/**
	  * @param connection Database connection (implicit)
	  * @return The largest available row id
	  */
	def max(implicit connection: Connection) = first(OrderBy.descending(index))
	
	
	// IMPLEMENTED	---------------------------
	
	override protected def read(condition: Option[Condition], order: Option[OrderBy])(implicit connection: Connection) =
	{
		val statement = Select.index(target, table) + condition.map { Where(_) } + order + Limit(1)
		valueToId(connection(statement).firstValue)
	}
	
	override def filter(additionalCondition: Condition): SingleIdAccess[ID] = new Filtered(additionalCondition)
	
	
	// NESTED	------------------------------
	
	private class Filtered(condition: Condition) extends SingleIdAccess[ID]
	{
		override def target = SingleIdAccess.this.target
		
		override def valueToId(value: Value) = SingleIdAccess.this.valueToId(value)
		
		override def table = SingleIdAccess.this.table
		
		override def globalCondition = Some(SingleIdAccess.this.mergeCondition(condition))
	}
}
package utopia.vault.nosql.access.single.column

import utopia.vault.database.Connection
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.access.single.SingleAccess
import utopia.vault.nosql.access.template.column.ColumnAccess
import utopia.vault.sql.{Condition, JoinType, Limit, OrderBy, Select, Where}

/**
  * Used for accessing individual column values in a table
  * @author Mikko Hilpinen
  * @since 11.7.2021, v1.8
  */
trait SingleColumnAccess[+V] extends ColumnAccess[Option[V], Option[V]] with SingleAccess[V]
{
	// COMPUTED	-------------------------------
	
	/**
	  * @param connection Database connection (implicit)
	  * @return The smallest available row id
	  */
	def min(implicit connection: Connection) = firstUsing(OrderBy.ascending(column))
	/**
	  * @param connection Database connection (implicit)
	  * @return The largest available row id
	  */
	def max(implicit connection: Connection) = firstUsing(OrderBy.descending(column))
	
	
	// IMPLEMENTED	---------------------------
	
	override protected def read(condition: Option[Condition], order: Option[OrderBy], joins: Seq[Joinable],
	                            joinType: JoinType)
	                           (implicit connection: Connection) =
	{
		if (condition.exists { _.isAlwaysFalse })
			None
		else {
			val statement = Select(joins.foldLeft(target) { _.join(_, joinType) }, column) +
				condition.map { Where(_) } + order + Limit(1)
			parseValue(connection(statement).firstValue)
		}
	}
}

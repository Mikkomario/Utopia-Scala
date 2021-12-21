package utopia.vault.nosql.access.many.column

import utopia.vault.database.Connection
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.access.many.ManyAccess
import utopia.vault.nosql.access.template.column.ColumnAccess
import utopia.vault.sql.{Condition, JoinType, OrderBy, Select, Where}

/**
 * Used for accessing multiple values of a single column at a time
 * @author Mikko Hilpinen
 * @since 11.7.2021, v1.8
 */
trait ManyColumnAccess[+V] extends ColumnAccess[V, Vector[V]] with ManyAccess[V]
{
	// COMPUTED ---------------------------
	
	/**
	 * @param connection Implicit database connection
	 * @return An iterator that returns all ids accessible from this access point. The iterator is usable
	 *         only while the connection is kept open.
	 */
	def iterator(implicit connection: Connection) =
		connection.iterator(Select.index(target, table) + globalCondition.map { Where(_) })
			.flatMap { _.rowValues.map(parseValue) }
	
	
	// IMPLEMENTED	-----------------------
	
	override protected def read(condition: Option[Condition], order: Option[OrderBy],
	                            joins: Seq[Joinable], joinType: JoinType)(implicit connection: Connection) =
	{
		val statement = Select(joins.foldLeft(target) { _.join(_, joinType) }, column) +
			condition.map { Where(_) } + order
		connection(statement).rowValues.map(parseValue).distinct
	}
}



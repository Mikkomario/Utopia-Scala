package utopia.vault.nosql.access.many.model

import utopia.flow.datastructure.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.access.many.ManyAccess
import utopia.vault.nosql.access.template.model.DistinctModelAccess
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.{Condition, JoinType, OrderBy, Select, Where}

/**
 * Used for accessing multiple models at a time from DB
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait ManyModelAccess[+A] extends ManyAccess[A]
	with DistinctModelAccess[A, Vector[A], Vector[Value]]
{
	// COMPUTED --------------------------------
	
	/**
	 * @param connection DB Connection (implicit)
	 * @return An iterator to all models accessible through this access point. The iterator is valid
	 *         only while the connection is kept open.
	 */
	def iterator(implicit connection: Connection) = factory.iterator(globalCondition)
	
	
	// IMPLEMENTED  ----------------------------
	
	override protected def read(condition: Option[Condition], order: Option[OrderBy], joins: Seq[Joinable],
	                            joinType: JoinType)(implicit connection: Connection) =
	{
		condition match {
			case Some(condition) => factory.findMany(condition, order, joins, joinType)
			case None => factory.getAll(order)
		}
	}
	
	override protected def readColumn(column: Column, additionalCondition: Option[Condition] = None,
	                                  order: Option[OrderBy] = None, joins: Seq[Joinable] = Vector(),
	                                  joinType: JoinType = Inner)(implicit connection: Connection) =
	{
		// Forms the query first
		val statement = Select(joins.foldLeft(target) { _.join(_, joinType) }, column) +
			additionalCondition.map { Where(_) } + order
		// Applies the query and parses results
		connection(statement).rowValues
	}
	
	
	// OTHER    -------------------------------
	
	/**
	 * @param order Order to use in the results
	 * @param connection DB Connection (implicit)
	 * @return An iterator to all models accessible from this access point. The iterator is usable
	 *         only while the connection is kept open.
	 */
	def orderedIterator(order: OrderBy)(implicit connection: Connection) =
		factory.iterator(globalCondition, Some(order))
}
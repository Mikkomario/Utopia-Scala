package utopia.vault.nosql.access.single.model

import utopia.flow.generic.model.immutable.Value
import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.access.single.SingleAccess
import utopia.vault.nosql.access.template.model.ModelAccess
import utopia.vault.nosql.factory.row.FromRowFactory
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.OrderDirection.{Ascending, Descending}
import utopia.vault.sql.{Condition, JoinType, Limit, OrderBy, OrderDirection, Select, Where}

/**
 * Used for accessing individual models from DB
 * @author Mikko Hilpinen
 * @since 30.1.2020, v1.4
 */
trait SingleModelAccess[+A] extends SingleAccess[A] with ModelAccess[A, Option[A], Value]
{
	// IMPLEMENTED  -------------------------------
	
	override protected def read(condition: Option[Condition], order: Option[OrderBy], joins: Seq[Joinable],
	                            joinType: JoinType)
	                           (implicit connection: Connection) =
	{
		condition match {
			case Some(condition) => factory.find(condition, order, joins, joinType)
			case None =>
				factory match {
					case rowFactory: FromRowFactory[A] =>
						order match {
							case Some(order) => rowFactory.firstUsing(order)
							case None => rowFactory.any
						}
					case _ => factory.getAll(order).headOption // This is not recommended
				}
		}
	}
	
	override protected def readColumn(column: Column, additionalCondition: Option[Condition] = None,
	                                  order: Option[OrderBy] = None, joins: Seq[Joinable] = Vector(),
	                                  joinType: JoinType = Inner)
	                                 (implicit connection: Connection) =
	{
		// Forms the query first
		val statement = Select(joins.foldLeft(factory.target) { _.join(_, joinType) }, column) +
			mergeCondition(additionalCondition).map { Where(_) } + order.orElse(factory.defaultOrdering) + Limit(1)
		// Applies the query and parses results
		connection(statement).firstValue
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * Reads the value of a column from the first row when reading accessible model(s) using the specified ordering
	  * @param column Column to read
	  * @param order Ordering to use
	  * @param additionalCondition Additional filter condition to apply (optional)
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use (default = inner)
	  * @param connection Implicit DB connection
	  * @return Value of the specified column in the first accessible row when using the specified ordering
	  *         (and other search criteria)
	  */
	def firstColumnUsing(column: Column, order: OrderBy, additionalCondition: Option[Condition] = None,
	                     joins: Seq[Joinable] = Vector(), joinType: JoinType = Inner)
	                    (implicit connection: Connection) =
		readColumn(column, additionalCondition, Some(order), joins, joinType)
	
	/**
	  * Reads the value of a column from a max/min row based on another column
	  * @param readColumn Column to read
	  * @param orderingColumn Column to base the ordering upon
	  * @param orderDirection Ordering direction to use
	  * @param additionalCondition Additional filter condition to apply (optional)
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use (default = inner)
	  * @param connection Implicit DB connection
	  * @return Value of the specified column in the first accessible row when using the specified ordering
	  *         (and other search criteria)
	  */
	def topColumnBy(readColumn: Column, orderingColumn: Column, orderDirection: OrderDirection,
	                additionalCondition: Option[Condition] = None, joins: Seq[Joinable] = Vector(),
	                joinType: JoinType = Inner)
	               (implicit connection: Connection) =
		firstColumnUsing(readColumn, OrderBy(orderingColumn, orderDirection), additionalCondition, joins, joinType)
	
	/**
	  * Reads the value of a column from a row with the maximum value in another column
	  * @param readColumn Column to read
	  * @param orderingColumn Column to base the ordering upon
	  * @param additionalCondition Additional filter condition to apply (optional)
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use (default = inner)
	  * @param connection Implicit DB connection
	  * @return Value of the specified column in the row with the maximum value in the other column
	  */
	def maxColumnBy(readColumn: Column, orderingColumn: Column, additionalCondition: Option[Condition] = None,
	                joins: Seq[Joinable] = Vector(), joinType: JoinType = Inner)
	               (implicit connection: Connection) =
		topColumnBy(readColumn, orderingColumn, Descending, additionalCondition, joins, joinType)
	/**
	  * Reads the value of a column from a row with the minimum value in another column
	  * @param readColumn Column to read
	  * @param orderingColumn Column to base the ordering upon
	  * @param additionalCondition Additional filter condition to apply (optional)
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use (default = inner)
	  * @param connection Implicit DB connection
	  * @return Value of the specified column in the row with the minimum value in the other column
	  */
	def minColumnBy(readColumn: Column, orderingColumn: Column, additionalCondition: Option[Condition] = None,
	                joins: Seq[Joinable] = Vector(), joinType: JoinType = Inner)
	               (implicit connection: Connection) =
		topColumnBy(readColumn, orderingColumn, Ascending, additionalCondition, joins, joinType)
	
	/**
	  * Reads the largest accessible value of a column
	  * @param column Column to read
	  * @param additionalCondition Additional filter condition to apply (optional)
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use (default = inner)
	  * @param connection Implicit DB connection
	  * @return Largest accessible value of that column
	  */
	def maxColumn(column: Column, additionalCondition: Option[Condition] = None,
	              joins: Seq[Joinable] = Vector(), joinType: JoinType = Inner)
	             (implicit connection: Connection) =
		maxColumnBy(column, column, additionalCondition, joins, joinType)
	/**
	  * Reads the smallest accessible value of a column
	  * @param column Column to read
	  * @param additionalCondition Additional filter condition to apply (optional)
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use (default = inner)
	  * @param connection Implicit DB connection
	  * @return Smallest accessible value of that column
	  */
	def minColumn(column: Column, additionalCondition: Option[Condition] = None,
	              joins: Seq[Joinable] = Vector(), joinType: JoinType = Inner)
	             (implicit connection: Connection) =
		minColumnBy(column, column, additionalCondition, joins, joinType)
}
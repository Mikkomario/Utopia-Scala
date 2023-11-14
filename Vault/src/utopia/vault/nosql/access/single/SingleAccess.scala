package utopia.vault.nosql.access.single

import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.model.template.Joinable
import utopia.vault.nosql.access.template.Access
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.OrderDirection.{Ascending, Descending}
import utopia.vault.sql.{Condition, JoinType, OrderBy, OrderDirection}

/**
  * A common trait for access points that return individual instances
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  */
trait SingleAccess[+A] extends Access[Option[A]]
{
	// OTHER	---------------------
	
	/**
	  * Reads the first accessible row when using the specified ordering
	  * @param ordering Ordering to apply
	  * @param additionalCondition Additional search condition to apply (optional)
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use (default = Inner)
	  * @param connection Implicit DB Connection
	  * @return The first accessible item when using the specified ordering (and other search criteria)
	  */
	def firstUsing(ordering: OrderBy, additionalCondition: Option[Condition] = None, joins: Seq[Joinable] = Vector(),
	               joinType: JoinType = Inner)
	              (implicit connection: Connection) =
		read(mergeCondition(additionalCondition), Some(ordering), joins, joinType)
	/**
	  * Reads the first accessible row when using the specified ordering and a search condition
	  * @param ordering Ordering to apply
	  * @param condition A search condition / filter to apply
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use (default = Inner)
	  * @param connection Implicit DB Connection
	  * @return The first accessible item when using the specified ordering and search condition
	  */
	def findFirstUsing(ordering: OrderBy, condition: Condition, joins: Seq[Joinable] = Vector(),
	                   joinType: JoinType = Inner)(implicit connection: Connection) =
		firstUsing(ordering, Some(condition), joins, joinType)
	
	/**
	  * Reads the item with a min/max value in a specific column
	  * @param orderingColumn Column to base ordering upon
	  * @param orderDirection Order direction to use
	  * @param additionalCondition Additional search condition to apply (optional)
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use (default = Inner)
	  * @param connection Implicit DB Connection
	  * @return The item with a min/max value in the specified column
	  */
	def topBy(orderingColumn: Column, orderDirection: OrderDirection, additionalCondition: Option[Condition] = None,
	          joins: Seq[Joinable] = Vector(), joinType: JoinType = Inner)
	         (implicit connection: Connection) =
		firstUsing(OrderBy(orderingColumn, orderDirection), additionalCondition, joins, joinType)
	
	/**
	  * Reads the item with the smallest value in the specified column
	  * @param column              Ordering column
	  * @param additionalCondition an additional search condition to apply (optional)
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use (default = Inner)
	  * @param connection          Database connection (implicit)
	  * @return The item with the smallest value in the specified column
	  */
	def minBy(column: Column, additionalCondition: Option[Condition] = None,
	          joins: Seq[Joinable] = Vector(), joinType: JoinType = Inner)
	         (implicit connection: Connection) =
		topBy(column, Ascending, additionalCondition, joins, joinType)
	/**
	  * The minimum value based on specified ordering
	  * @param propertyName Name of ordering property
	  * @param connection   Database connection (implicit)
	  * @return The smallest item based on provided ordering.
	  */
	def minBy(propertyName: String)(implicit connection: Connection): Option[A] =
		minBy(table(propertyName))
	
	/**
	  * Reads the item with the largest value in the specified column
	  * @param column              Ordering column
	  * @param additionalCondition an additional search condition to apply (optional)
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use (default = Inner)
	  * @param connection          Database connection (implicit)
	  * @return The item with the largest value in the specified column
	  */
	def maxBy(column: Column, additionalCondition: Option[Condition] = None,
	          joins: Seq[Joinable] = Vector(), joinType: JoinType = Inner)
	         (implicit connection: Connection) =
		topBy(column, Descending, additionalCondition, joins, joinType)
	/**
	  * The maximum value based on specified ordering
	  * @param propertyName Name of ordering property
	  * @param connection   Database connection (implicit)
	  * @return The largest item based on provided ordering.
	  */
	def maxBy(propertyName: String)(implicit connection: Connection): Option[A] = maxBy(table(propertyName))
}

package utopia.vault.nosql.access.single

import utopia.vault.database.Connection
import utopia.vault.model.immutable.Column
import utopia.vault.nosql.access.template.Access
import utopia.vault.sql.OrderDirection.{Ascending, Descending}
import utopia.vault.sql.{Condition, OrderBy, OrderDirection}

/**
  * A common trait for access points that return individual instances
  * @author Mikko Hilpinen
  * @since 30.1.2020, v1.4
  */
trait SingleAccess[+A] extends Access[Option[A]]
{
	// OTHER	---------------------
	
	/**
	  * @param ordering            Ordering used
	  * @param additionalCondition An additional search confition used (optional)
	  * @param connection          Implicit database connection
	  * @return The first item in specified ordering that satisfies the used search condition
	  */
	def first(ordering: OrderBy, additionalCondition: Option[Condition])(implicit connection: Connection) =
		read(mergeCondition(additionalCondition), Some(ordering))
	/**
	  * @param ordering   Ordering used
	  * @param connection Implicit database connection
	  * @return The first item in specified ordering
	  */
	def first(ordering: OrderBy)(implicit connection: Connection): Option[A] = first(ordering, None)
	/**
	  * @param ordering   Ordering used
	  * @param condition  An additional search confition used
	  * @param connection Implicit database connection
	  * @return The first item in specified ordering that satisfies the used search condition
	  */
	def first(ordering: OrderBy, condition: Condition)(implicit connection: Connection): Option[A] =
		first(ordering, Some(condition))
	
	/**
	  * The "top" value based on specified ordering
	  * @param orderColumn         Ordering column
	  * @param orderDirection      Ordering direction
	  * @param additionalCondition a search condition
	  * @param connection          Database connection (implicit)
	  * @return The "top" (first result) item based on provided ordering and search condition.
	  */
	def top(orderColumn: Column, orderDirection: OrderDirection, additionalCondition: Option[Condition])
	       (implicit connection: Connection) = first(OrderBy(orderColumn, orderDirection), additionalCondition)
	/**
	  * The "top" value based on specified ordering
	  * @param orderColumn    Ordering column
	  * @param orderDirection Ordering direction
	  * @param connection     Database connection (implicit)
	  * @return The "top" (first result) item based on provided ordering.
	  */
	def top(orderColumn: Column, orderDirection: OrderDirection)(implicit connection: Connection): Option[A] =
		top(orderColumn, orderDirection, None)
	
	/**
	  * The minimum value based on specified ordering
	  * @param column              Ordering column
	  * @param additionalCondition a search condition
	  * @param connection          Database connection (implicit)
	  * @return The smallest item based on provided ordering and search condition.
	  */
	def minBy(column: Column, additionalCondition: Option[Condition])(implicit connection: Connection) =
		top(column, Ascending, additionalCondition)
	/**
	  * The minimum value based on specified ordering
	  * @param column     Ordering column
	  * @param connection Database connection (implicit)
	  * @return The smallest item based on provided ordering.
	  */
	def minBy(column: Column)(implicit connection: Connection): Option[A] = minBy(column, None)
	/**
	  * The minimum value based on specified ordering
	  * @param propertyName        Name of ordering property
	  * @param additionalCondition a search condition
	  * @param connection          Database connection (implicit)
	  * @return The smallest item based on provided ordering and search condition.
	  */
	def minBy(propertyName: String, additionalCondition: Option[Condition])(implicit connection: Connection): Option[A] =
		minBy(table(propertyName), additionalCondition)
	/**
	  * The minimum value based on specified ordering
	  * @param propertyName Name of ordering property
	  * @param connection   Database connection (implicit)
	  * @return The smallest item based on provided ordering.
	  */
	def minBy(propertyName: String)(implicit connection: Connection): Option[A] = minBy(propertyName, None)
	
	/**
	  * The maximum value based on specified ordering
	  * @param column              Ordering column
	  * @param additionalCondition a search condition
	  * @param connection          Database connection (implicit)
	  * @return The largest item based on provided ordering and search condition.
	  */
	def maxBy(column: Column, additionalCondition: Option[Condition])(implicit connection: Connection) =
		top(column, Descending, additionalCondition)
	/**
	  * The maximum value based on specified ordering
	  * @param column     Ordering column
	  * @param connection Database connection (implicit)
	  * @return The largest item based on provided ordering.
	  */
	def maxBy(column: Column)(implicit connection: Connection): Option[A] = maxBy(column, None)
	/**
	  * The maximum value based on specified ordering
	  * @param propertyName        Name of ordering property
	  * @param additionalCondition a search condition
	  * @param connection          Database connection (implicit)
	  * @return The largest item based on provided ordering and search condition.
	  */
	def maxBy(propertyName: String, additionalCondition: Option[Condition])(implicit connection: Connection): Option[A] =
		maxBy(table(propertyName), additionalCondition)
	/**
	  * The maximum value based on specified ordering
	  * @param propertyName Name of ordering property
	  * @param connection   Database connection (implicit)
	  * @return The largest item based on provided ordering.
	  */
	def maxBy(propertyName: String)(implicit connection: Connection): Option[A] = maxBy(propertyName, None)
}

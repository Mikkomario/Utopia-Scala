package utopia.vault.nosql.factory.row

import utopia.flow.generic.casting.ValueConversions._
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.ComparisonOperator
import utopia.vault.model.enumeration.ComparisonOperator.{Larger, LargerOrEqual, Smaller, SmallerOrEqual}
import utopia.vault.model.immutable.DbPropertyDeclaration
import utopia.vault.model.template.Joinable
import utopia.vault.sql.JoinType.Inner
import utopia.vault.sql.OrderDirection.Descending
import utopia.vault.sql.{Condition, JoinType, OrderBy, OrderDirection}

import java.time.Instant

/**
  * A common trait for factories that track row creation time
  * @author Mikko Hilpinen
  * @since 1.2.2020, v1.4
  */
trait FromRowFactoryWithTimestamps[+A] extends FromRowFactory[A]
{
	// ABSTRACT	------------------------
	
	/**
	  * @return Declaration for the timestamp property
	  */
	def timestamp: DbPropertyDeclaration
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return Name of the property that represents item creation time
	  */
	@deprecated("Please use .timestamp.name instead", "v1.19")
	def creationTimePropertyName: String = timestamp.name
	/**
	  * @return Column that specifies row creation time
	  */
	@deprecated("Please use .timestamp.column instead", "v1.19")
	def creationTimeColumn = timestamp.column
	/**
	  * @return Ordering that uses row creation time (descending)
	  */
	@deprecated("Renamed to timestampOrdering", "v1.19")
	def creationTimeOrdering = timestampOrdering
	
	/**
	  * @return A descending timestamp-based ordering.
	  *         I.e. an ordering that returns items from the latest to the earliest.
	  */
	def timestampOrdering = directionalTimestampOrdering(Descending)
	
	/**
	  * @return The latest recorded item
	  */
	def latest(implicit connection: Connection) = maxBy(timestamp.column)
	/**
	  * @return The earliest recorded item
	  */
	def earliest(implicit connection: Connection) = minBy(timestamp.column)
	
	
	// IMPLEMENTED	---------------------
	
	/**
	  * @return Default ordering used by this factory (by default returns first the latest items)
	  */
	override def defaultOrdering: Option[OrderBy] = Some(timestampOrdering)
	
	
	// OTHER	-------------------------
	
	/**
	  * @param direction Desired ordering direction
	  * @return A row timestamp -based ordering with the specified ordering direction
	  */
	def directionalTimestampOrdering(direction: OrderDirection) = OrderBy(timestamp, direction)
	
	/**
	  * @param where      A search condition
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use in joins (default = inner)
	  * @param connection DB Connection (implicit)
	  * @return Latest item in database that satisfies the specified condition
	  */
	def findLatest(where: Condition, joins: Seq[Joinable] = Vector(), joinType: JoinType = Inner)
	              (implicit connection: Connection) =
		findMaxBy(timestamp.column, where, joins, joinType)
	/**
	  * @param where      A search condition
	  * @param joins Joins to apply (default = empty)
	  * @param joinType Join type to use in joins (default = inner)
	  * @param connection DB Connection (implicit)
	  * @return Earliest item in database that satisfies the specified condition
	  */
	def findEarliest(where: Condition, joins: Seq[Joinable] = Vector(), joinType: JoinType = Inner)
	                (implicit connection: Connection) =
		findMinBy(timestamp.column, where, joins, joinType)
	
	/**
	  * Takes latest n items from the database
	  * @param maxNumberOfItems Maximum number of items to return
	  * @param connection       DB Connection (implicit)
	  * @return Latest 'maxNumberOfItems' items from database
	  */
	def takeLatest(maxNumberOfItems: Int)(implicit connection: Connection) =
		take(maxNumberOfItems, directionalTimestampOrdering(Descending))
	
	/**
	  * Finds a specific number of latest items that satisfy specified search condition
	  * @param condition        A search condition
	  * @param maxNumberOfItems Maximum number of items to return
	  * @param connection       DB Connection
	  * @return Latest 'maxNumberOfItems' items that satisfy specified condition
	  */
	def takeLatestWhere(condition: Condition, maxNumberOfItems: Int)(implicit connection: Connection) =
		take(maxNumberOfItems, directionalTimestampOrdering(Descending), Some(condition))
	
	/**
	  * @param threshold Time threshold
	  * @param operator  An operator used for comparing row creation times with specified threshold
	  * @return A condition that accepts rows based on specified threshold and operator
	  */
	def creationCondition(threshold: Instant, operator: ComparisonOperator) =
		timestamp.column.makeCondition(operator, threshold)
	/**
	  * @param threshold   Time threshold
	  * @param isInclusive Whether the threshold should be included in return values (default = false)
	  * @return A condition that accepts rows that were created before the specified time threshold
	  */
	def createdBeforeCondition(threshold: Instant, isInclusive: Boolean = false) =
		creationCondition(threshold, if (isInclusive) SmallerOrEqual else Smaller)
	/**
	  * @param threshold   Time threshold
	  * @param isInclusive Whether the threshold should be included in return values (default = false)
	  * @return A condition that accepts rows that were created after the specified time threshold
	  */
	def createdAfterCondition(threshold: Instant, isInclusive: Boolean = false) =
		creationCondition(threshold, if (isInclusive) LargerOrEqual else Larger)
	/**
	  * @param start Minimum creation time
	  * @param end   Maximum creation time
	  * @return A condition that accepts items that were created between 'start' and 'end'
	  */
	def createdBetweenCondition(start: Instant, end: Instant) = timestamp.isBetween(start, end)
	
	/**
	  * @param threshold           Time threshold
	  * @param maxNumberOfItems    Maximum number of items to return
	  * @param isInclusive         Whether the threshold should be included in return values (default = false)
	  * @param additionalCondition Additional search condition applied (default = None)
	  * @return Up to 'maxNumberOfItems' items that were created before the specified time threshold
	  */
	def createdBefore(threshold: Instant, maxNumberOfItems: Int, isInclusive: Boolean = false,
	                  additionalCondition: Option[Condition] = None)(implicit connection: Connection) =
	{
		val condition = createdBeforeCondition(threshold, isInclusive) && additionalCondition
		take(maxNumberOfItems, directionalTimestampOrdering(Descending), Some(condition))
	}
	
	/**
	  * @param threshold           Time threshold
	  * @param additionalCondition Additional search condition applied (default = None)
	  * @param isInclusive         Whether the threshold should be included in return values (default = false)
	  * @param connection          DB Connection (implicit)
	  * @return All items that were created after the specified time threshold
	  */
	def createdAfter(threshold: Instant, additionalCondition: Option[Condition] = None, isInclusive: Boolean = false)
	                (implicit connection: Connection) =
	{
		val condition = createdAfterCondition(threshold, isInclusive) && additionalCondition
		findMany(condition)
	}
	
	/**
	  * @param threshold   A time threshold
	  * @param isInclusive Whether a possible item created exactly at the time threshold should be returned
	  * @param connection  DB Connection (implicit)
	  * @return The last item that was created before the specified time threshold (None if no items were found)
	  */
	def latestBefore(threshold: Instant, isInclusive: Boolean = false)(implicit connection: Connection) =
		find(createdBeforeCondition(threshold, isInclusive))
	
	/**
	  * @param threshold   A time threshold
	  * @param isInclusive Whether a possible item created exactly at the time threshold should be returned
	  * @param connection  DB Connection (implicit)
	  * @return The first item that was created after the specified time threshold (None if no items were found)
	  */
	def firstAfter(threshold: Instant, isInclusive: Boolean = false)(implicit connection: Connection) =
		find(createdAfterCondition(threshold, isInclusive))
}

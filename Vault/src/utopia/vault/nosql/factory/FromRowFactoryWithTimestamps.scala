package utopia.vault.nosql.factory

import java.time.Instant

import utopia.flow.generic.ValueConversions._
import utopia.vault.sql.SqlExtensions._
import utopia.vault.database.Connection
import utopia.vault.model.enumeration.ComparisonOperator
import utopia.vault.model.enumeration.ComparisonOperator.{Larger, LargerOrEqual, Smaller, SmallerOrEqual}
import utopia.vault.sql.{Condition, OrderBy}

/**
  * A common trait for factories that track row creation time
  * @author Mikko Hilpinen
  * @since 1.2.2020, v1.4
  */
trait FromRowFactoryWithTimestamps[+A] extends FromRowFactory[A]
{
	// ABSTRACT	------------------------
	
	/**
	  * @return Name of the property that represents item creation time
	  */
	def creationTimePropertyName: String
	
	
	// COMPUTED	-------------------------
	
	/**
	  * @return Column that specifies row creation time
	  */
	def creationTimeColumn = table(creationTimePropertyName)
	
	/**
	  * @return Default ordering used by this factory (by default returns first the latest items)
	  */
	def defaultOrdering = OrderBy.descending(creationTimeColumn)
	
	/**
	  * @return The latest recorded item
	  */
	def latest(implicit connection: Connection) = getMax(creationTimeColumn)
	
	/**
	  * @return The earliest recorded item
	  */
	def earliest(implicit connection: Connection) = getMin(creationTimeColumn)
	
	
	// IMPLEMENTED	---------------------
	
	override def get(where: Condition, order: Option[OrderBy])(implicit connection: Connection) =
	{
		// Uses default ordering if no other ordering has been specified
		super.get(where, Some(order.getOrElse(defaultOrdering)))
	}
	
	
	// OTHER	-------------------------
	
	/**
	  * @param where A search condition
	  * @param connection DB Connection (implicit)
	  * @return Latest item in database that satisfies the specified condition
	  */
	def findLatest(where: Condition)(implicit connection: Connection) = getMax(creationTimeColumn, where)
	
	/**
	  * @param where A search condition
	  * @param connection DB Connection (implicit)
	  * @return Earliest item in database that satisfies the specified condition
	  */
	def findEarliest(where: Condition)(implicit connection: Connection) = getMin(creationTimeColumn, where)
	
	/**
	  * Takes latest n items from the database
	  * @param maxNumberOfItems Maximum number of items to return
	  * @param connection DB Connection (implicit)
	  * @return Latest 'maxNumberOfItems' items from database
	  */
	def takeLatest(maxNumberOfItems: Int)(implicit connection: Connection) = take(maxNumberOfItems, defaultOrdering)
	
	/**
	  * Finds a specific number of latest items that satisfy specified search condition
	  * @param condition A search condition
	  * @param maxNumberOfItems Maximum number of items to return
	  * @param connection DB Connection
	  * @return Latest 'maxNumberOfItems' items that satisfy specified condition
	  */
	def takeLatestWhere(condition: Condition, maxNumberOfItems: Int)(implicit connection: Connection) =
		take(maxNumberOfItems, defaultOrdering, Some(condition))
	
	/**
	  * @param threshold Time threshold
	  * @param operator An operator used for comparing row creation times with specified threshold
	  * @return A condition that accepts rows based on specified threshold and operator
	  */
	def creationCondition(threshold: Instant, operator: ComparisonOperator) = creationTimeColumn.makeCondition(operator, threshold)
	
	/**
	  * @param threshold Time threshold
	  * @param isInclusive Whether the threshold should be included in return values (default = false)
	  * @return A condition that accepts rows that were created before the specified time threshold
	  */
	def createdBeforeCondition(threshold: Instant, isInclusive: Boolean = false) = creationCondition(threshold,
		if (isInclusive) SmallerOrEqual else Smaller)
	
	/**
	  * @param threshold Time threshold
	  * @param isInclusive Whether the threshold should be included in return values (default = false)
	  * @return A condition that accepts rows that were created after the specified time threshold
	  */
	def createdAfterCondition(threshold: Instant, isInclusive: Boolean = false) = creationCondition(threshold,
		if (isInclusive) LargerOrEqual else Larger)
	
	/**
	  * @param start Minimum creation time
	  * @param end Maximum creation time
	  * @return A condition that accepts items that were created between 'start' and 'end'
	  */
	def createdBetweenCondition(start: Instant, end: Instant) = creationTimeColumn.isBetween(start, end)
	
	/**
	  * @param threshold Time threshold
	  * @param maxNumberOfItems Maximum number of items to return
	  * @param isInclusive Whether the threshold should be included in return values (default = false)
	  * @param additionalCondition Additional search condition applied (default = None)
	  * @return Up to 'maxNumberOfItems' items that were created before the specified time threshold
	  */
	def createdBefore(threshold: Instant, maxNumberOfItems: Int, isInclusive: Boolean = false,
					  additionalCondition: Option[Condition] = None)(implicit connection: Connection) =
	{
		val condition = createdBeforeCondition(threshold, isInclusive) && additionalCondition
		take(maxNumberOfItems, defaultOrdering, Some(condition))
	}
	
	/**
	  * @param threshold Time threshold
	  * @param isInclusive Whether the threshold should be included in return values (default = false)
	  * @param additionalCondition Additional search condition applied (default = None)
	  * @param connection DB Connection (implicit)
	  * @return All items that were created after the specified time threshold
	  */
	def createdAfter(threshold: Instant, isInclusive: Boolean = false, additionalCondition: Option[Condition] = None)
					(implicit connection: Connection) =
	{
		val condition = createdAfterCondition(threshold, isInclusive) && additionalCondition
		getMany(condition)
	}
	
	/**
	  * @param threshold A time threshold
	  * @param isInclusive Whether a possible item created exactly at the time threshold should be returned
	  * @param connection DB Connection (implicit)
	  * @return The last item that was created before the specified time threshold (None if no items were found)
	  */
	def latestBefore(threshold: Instant, isInclusive: Boolean = false)(implicit connection: Connection) =
		get(createdBeforeCondition(threshold, isInclusive))
	
	/**
	  * @param threshold A time threshold
	  * @param isInclusive Whether a possible item created exactly at the time threshold should be returned
	  * @param connection DB Connection (implicit)
	  * @return The first item that was created after the specified time threshold (None if no items were found)
	  */
	def firstAfter(threshold: Instant, isInclusive: Boolean = false)(implicit connection: Connection) =
		get(createdAfterCondition(threshold, isInclusive))
}

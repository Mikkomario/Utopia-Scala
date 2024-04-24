package utopia.vault.nosql.view

import utopia.flow.collection.immutable.range.HasInclusiveOrderedEnds
import utopia.vault.database.Connection
import utopia.vault.nosql.access.single.model.distinct.{LatestModelAccess, LatestOrEarliestModelAccess}
import utopia.vault.nosql.factory.row.FromTimelineRowFactory

import java.time.Instant

/**
  * Common trait for timeline-based views and access points
  * @author Mikko Hilpinen
  * @since 18.2.2022
  */
trait ChronoRowFactoryView[+A, +Sub] extends RowFactoryView[A] with FilterableView[Sub]
{
	// ABSTRACT --------------------------------
	
	override def factory: FromTimelineRowFactory[A]
	
	
	// COMPUTED --------------------------------
	
	/**
	  * @return Access to the latest accessible item only
	  */
	def latest = LatestModelAccess[A](factory, accessCondition)
	/**
	  * @return Access to the earliest accessible item only
	  */
	def earliest = LatestOrEarliestModelAccess.earliest(factory, accessCondition)
	
	
	// OTHER    --------------------------------
	
	/**
	  * @param threshold A time threshold
	  * @param inclusive Whether that threshold is inclusive (true) or exclusive (false, default)
	  * @return A copy of this access point targeting only items which were created before the specified time threshold
	  */
	def before(threshold: Instant, inclusive: Boolean = false) =
		filter(factory.beforeCondition(threshold, inclusive))
	/**
	  * @param threshold A time threshold
	  * @param inclusive Whether that threshold is inclusive (true) or exclusive (false, default)
	  * @return A copy of this access point targeting only items which were created before the specified time threshold
	  */
	@deprecated("Please use .before(...) instead", "v1.19")
	def createdBefore(threshold: Instant, inclusive: Boolean = false) = before(threshold, inclusive)
	/**
	  * @param threshold A time threshold
	  * @param inclusive Whether that threshold is inclusive (true) or exclusive (false, default)
	  * @return A copy of this access point targeting only items which were created after the specified time threshold
	  */
	def after(threshold: Instant, inclusive: Boolean = false) = filter(factory.afterCondition(threshold, inclusive))
	/**
	  * @param threshold A time threshold
	  * @param inclusive Whether that threshold is inclusive (true) or exclusive (false, default)
	  * @return A copy of this access point targeting only items which were created after the specified time threshold
	  */
	@deprecated("Please use .after(...) instead", "v1.19")
	def createdAfter(threshold: Instant, inclusive: Boolean = false) = after(threshold, inclusive)
	/**
	  * @param start Smallest included time
	  * @param end Largest included time
	  * @return A copy of this access point targeting only items which were created during the specified time period
	  */
	def between(start: Instant, end: Instant) = filter(factory.betweenCondition(start, end))
	/**
	  * @param start Smallest included time
	  * @param end Largest included time
	  * @return A copy of this access point targeting only items which were created during the specified time period
	  */
	@deprecated("Please use .between(...) instead", "v1.19")
	def createdBetween(start: Instant, end: Instant) = between(start, end)
	/**
	  * @param period A time period
	  * @return Access to items that were created during the specified time period
	  */
	def during(period: HasInclusiveOrderedEnds[Instant]) = {
		val minMax = period.minMax
		between(minMax.first, minMax.second)
	}
	/**
	  * @param period A time period
	  * @return Access to items that were created during the specified time period
	  */
	@deprecated("Please use .during(...) instead", "v1.19")
	def createdDuring(period: HasInclusiveOrderedEnds[Instant]) = during(period)
	
	/**
	  * @param n The number of items to return
	  * @param c Implicit DB connection
	  * @return The latest 'n' accessible items, based on their creation time
	  */
	def takeLatest(n: Int)(implicit c: Connection) = take(n, factory.timestampOrdering)
	/**
	  * @param n The number of items to return
	  * @param c Implicit DB connection
	  * @return The earliest 'n' accessible items, based on their creation time
	  */
	def takeEarliest(n: Int)(implicit c: Connection) = take(n, factory.timestampOrdering.ascending)
	
	/**
	  * @param threshold A time threshold
	  * @param inclusive Whether the time threshold should be considered inclusive (default = false)
	  * @param c Implicit DB connection
	  * @return Whether any items were added after the specified time threshold
	  */
	def isAfter(threshold: Instant, inclusive: Boolean = false)(implicit c: Connection) =
		exists(factory.afterCondition(threshold, inclusive))
	/**
	  * @param threshold A time threshold
	  * @param inclusive Whether the time threshold should be considered inclusive (default = false)
	  * @param c Implicit DB connection
	  * @return Whether any items were added after the specified time threshold
	  */
	@deprecated("Please use .isAfter(...) instead", "v1.19")
	def wasAddedAfter(threshold: Instant, inclusive: Boolean = false)(implicit c: Connection) =
		isAfter(threshold, inclusive)
}

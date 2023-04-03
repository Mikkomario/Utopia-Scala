package utopia.vault.nosql.view

import utopia.flow.collection.immutable.range.HasInclusiveEnds
import utopia.vault.database.Connection
import utopia.vault.nosql.factory.row.FromRowFactoryWithTimestamps

import java.time.Instant

/**
  * Common trait for views and access points
  * @author Mikko Hilpinen
  * @since 18.2.2022, v
  */
trait ChronoRowFactoryView[+A, +Sub] extends RowFactoryView[A] with FilterableView[Sub]
{
	// ABSTRACT --------------------------------
	
	override def factory: FromRowFactoryWithTimestamps[A]
	
	
	// OTHER    --------------------------------
	
	/**
	  * @param threshold A time threshold
	  * @param inclusive Whether that threshold is inclusive (true) or exclusive (false, default)
	  * @return A copy of this access point targeting only items which were created before the specified time threshold
	  */
	def createdBefore(threshold: Instant, inclusive: Boolean = false) =
		filter(factory.createdBeforeCondition(threshold, inclusive))
	/**
	  * @param threshold A time threshold
	  * @param inclusive Whether that threshold is inclusive (true) or exclusive (false, default)
	  * @return A copy of this access point targeting only items which were created after the specified time threshold
	  */
	def createdAfter(threshold: Instant, inclusive: Boolean = false) =
		filter(factory.createdAfterCondition(threshold, inclusive))
	/**
	  * @param start Smallest included time
	  * @param end Largest included time
	  * @return A copy of this access point targeting only items which were created during the specified time period
	  */
	def createdBetween(start: Instant, end: Instant) = filter(factory.createdBetweenCondition(start, end))
	/**
	  * @param period A time period
	  * @return Access to items that were created during the specified time period
	  */
	def createdDuring(period: HasInclusiveEnds[Instant]) = {
		val minMax = period.minMax
		createdBetween(minMax.first, minMax.second)
	}
	
	/**
	  * @param n The number of items to return
	  * @param c Implicit DB connection
	  * @return The latest 'n' accessible items, based on their creation time
	  */
	def takeLatest(n: Int)(implicit c: Connection) = take(n, factory.creationTimeOrdering)
	/**
	  * @param n The number of items to return
	  * @param c Implicit DB connection
	  * @return The earliest 'n' accessible items, based on their creation time
	  */
	def takeEarliest(n: Int)(implicit c: Connection) = take(n, factory.creationTimeOrdering.ascending)
	
	/**
	  * @param threshold A time threshold
	  * @param inclusive Whether the time threshold should be considered inclusive (default = false)
	  * @param c Implicit DB connection
	  * @return Whether any items were added after the specified time threshold
	  */
	def wasAddedAfter(threshold: Instant, inclusive: Boolean = false)(implicit c: Connection) =
		exists(factory.createdAfterCondition(threshold, inclusive))
}

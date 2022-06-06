package utopia.vault.nosql.view

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
}

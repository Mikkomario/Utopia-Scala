package utopia.scribe.core.model.cached.event

import utopia.flow.collection.immutable.range.Span

import java.time.Instant

/**
  * An event that is fired when the specified maximum logging limit is reached and the logging system shuts down.
  * @author Mikko Hilpinen
  * @since 3.8.2023, v1.0
  * @param limit The number of logging entries recorded before this event
  * @param timeSpan The time-span within which this limit was reached
  */
case class MaximumLogLimitReachedEvent(limit: Int, timeSpan: Span[Instant])
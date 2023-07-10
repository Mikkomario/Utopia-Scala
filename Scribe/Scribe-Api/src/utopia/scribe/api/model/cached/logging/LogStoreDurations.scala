package utopia.scribe.api.model.cached.logging

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.{IterableHasEnds, IterableSpan}
import utopia.flow.util.Mutate
import utopia.scribe.api.model.enumeration.CleanupOperation
import utopia.scribe.core.model.enumeration.Severity

import scala.concurrent.duration.Duration

object LogStoreDurations
{
	/**
	  * Instance where no store or merge durations have been specified
	  */
	val undefined = apply()
}

/**
  * Used for specifying, how long different kinds of issues and their occurrences are to be kept in the database,
  * and in which form
  * @author Mikko Hilpinen
  * @since 9.7.2023, v1.0
  * @param durations Store-durations that apply to specified severities.
  *                  Where no value has been specified, the less severe option will be applied, if available.
  */
case class LogStoreDurations(durations: Map[Severity, LogStoreDuration] = Map())
{
	// COMPUTED ---------------------------
	
	/**
	  * @return An iterator that returns all different store duration sets, mapped to the range of severities
	  *         to which they apply.
	  *         E.g. May return a duration for Debug to Warning, and then another value for Unrecoverable to Critical
	  */
	def specifiedRangesIterator = {
		Severity.values.iterator.groupBy(durations.get).flatMap { case (duration, severities) =>
			duration.map { d => IterableSpan(severities.ends) -> d }
		}
	}
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param severity Targeted severity level
	  * @return Store durations that apply on that severity level
	  */
	def apply(severity: Severity) =
		severity.andSmaller.findMap { durations.get }.getOrElse(LogStoreDuration.unspecified)
	/**
	  * @param operation Targeted cleanup operation
	  * @param severity Targeted severity level
	  * @return Store duration thresholds that apply to that operation on that severity level.
	  *         The meaning of these thresholds depends on the type of targeted cleanup operation
	  *         (See [[LogStoreDuration]] for more details).
	  */
	def apply(operation: CleanupOperation, severity: Severity): Vector[Pair[Duration]] = apply(severity)(operation)
	/**
	  * @param operation Targeted cleanup operation
	  * @param severity Targeted severity level
	  * @param duration Applicable store or inactivity duration (role depends on the targeted operation)
	  * @return Applicable merge or store duration. The meaning of this value differs based on the
	  *         targeted cleanup operation (See [[LogStoreDuration]] for more details).
	  */
	def apply(operation: CleanupOperation, severity: Severity, duration: Duration): Option[Duration] =
		apply(severity)(operation, duration)
	
	/**
	  * Maps store durations for the targeted severity levels
	  * @param severities Targeted severity levels
	  * @param f A mapping function for store durations
	  * @return Copy of these durations with mapped values
	  */
	def mapSeveries(severities: IterableOnce[Severity])(f: Mutate[LogStoreDuration]) =
		copy(durations = durations ++ severities.iterator.map { s => s -> f(apply(s)) })
	/**
	  * Maps store durations for the targeted severity levels
	  * @param from The first targeted severity level
	  * @param to The last targeted severity level
	  * @param f          A mapping function for store durations
	  * @return Copy of these durations with mapped values
	  */
	def mapSeverities(from: Severity, to: Severity)(f: Mutate[LogStoreDuration]): LogStoreDurations =
		mapSeveries(IterableHasEnds(from, to))(f)
	/**
	  * Maps store durations for the targeted severity level
	  * @param severity Targeted severity level
	  * @param f    A mapping function for store durations
	  * @return Copy of these durations with mapped values
	  */
	def mapSeverity(severity: Severity)(f: Mutate[LogStoreDuration]) =
		copy(durations = durations + (severity -> f(apply(severity))))
	
	/**
	  * @param operation Targeted cleanup operation
	  * @param severities Targeted severity levels
	  * @param thresholds Applied cleanup threshold durations
	  *                   (see [[LogStoreDuration]] for more details about the meaning/role of this parameter)
	  * @return Copy of this duration set with added thresholds
	  */
	def withThresholds(operation: CleanupOperation, severities: IterableOnce[Severity],
	                   thresholds: IterableOnce[Pair[Duration]]) =
		mapSeveries(severities) { _.withThresholds(operation, thresholds) }
}

package utopia.scribe.api.controller.logging

import utopia.flow.collection.immutable.range.Span
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.NotEmpty
import utopia.scribe.api.database.ScribeTables
import utopia.scribe.api.database.access.many.logging.issue.{DbIssues, DbManyIssueInstances}
import utopia.scribe.api.database.access.many.logging.issue_occurrence.DbIssueOccurrences
import utopia.scribe.api.database.access.many.logging.issue_variant.DbIssueVariants
import utopia.scribe.api.database.model.logging.IssueOccurrenceModel
import utopia.scribe.api.model.cached.logging.LogStoreDurations
import utopia.scribe.core.model.partial.logging.IssueOccurrenceData
import utopia.scribe.core.model.stored.logging.IssueOccurrence
import utopia.vault.database.Connection

import scala.concurrent.duration.Duration

/**
  * An interface for processes that reduce stored issue space in the database.
  * @author Mikko Hilpinen
  * @since 9.7.2023, v1.0
  */
object LogCleaner
{
	/**
	  * Performs log entry -cleaning by merging older entries into larger groups,
	  * and by deleting old entries of inactive issues
	  * @param durations Store durations that determine how merging and deletion function
	  * @param connection Implicit DB connection
	  */
	def apply(durations: LogStoreDurations)(implicit connection: Connection) = {
		val now = Now.toInstant
		// Prepares occurrence merging
		val mergeDataIter = durations.specifiedRangesIterator.flatMap { case (severities, durations) =>
			durations.mergeDurationRangesIterator.flatMap { case (durationRange, mergeGroupLength) =>
				// Finds the issue occurrences that match the specified severity levels
				// and occurred within the specified time period
				val issues = DbManyIssueInstances.withSeverityIn(severities).during(durationRange.mapTo { now - _ }).pull
				if (issues.exists { _.hasOccurred }) {
					// Merges occurrences within specific time periods, forming a single item
					val dataIter = mergeGroupLength.finite match {
						// Case: Finite merge group length => Forms possibly multiple groups
						case Some(mergeGroupLength) =>
							val veryFirstOccurrenceTime = issues
								.flatMap { _.earliestOccurrence.map { _.firstOccurrence } }.min
							val veryLastOccurrenceTime = issues
								.flatMap { _.latestOccurrence.map { _.lastOccurrence } }.max
							// Forms the time-spans which to merge together
							Iterator
								.iterate(Span(veryFirstOccurrenceTime, veryFirstOccurrenceTime + mergeGroupLength)) { s =>
									Span(s.end, s.end + mergeGroupLength)
								}
								.takeWhile { _.start < veryLastOccurrenceTime }
								.flatMap { range =>
									// Forms merged occurrences for each issue that occurred during the specified time-span
									issues.iterator.flatMap { _.variants }.flatMap { variant =>
										NotEmpty(variant.occurrences.filter { o => range.contains(o.lastOccurrence) })
											.map(mergeDataFrom)
									}
								}
							
						// Case: Infinite merge group length => Always forms a single group
						case None =>
							issues.iterator.flatMap { _.variants }.flatMap { variant =>
								NotEmpty(variant.occurrences).map(mergeDataFrom)
							}
					}
					dataIter.flatMap { _.toOption }
				}
				else
					None
			}
		}
		val (newOccurrenceData, removeOccurrenceIdSets) = mergeDataIter.split
		// Applies queued occurrence deletion
		DbIssueOccurrences(removeOccurrenceIdSets.iterator.flatten.toSet).delete()
		// Inserts the merged occurrences, so that they affect the deletion processes
		IssueOccurrenceModel.insert(newOccurrenceData)
		
		// Deletes occurrences that are too old
		// Targets issues by severity and length of inactivity
		durations.specifiedRangesIterator.foreach { case (severities, durations) =>
			durations.deletionThresholds.foreach { case Pair(requiredInactivity, deleteAfter) =>
				// If required inactivity or lifetime duration is infinite, no deletion occurs in practice
				requiredInactivity.finite.foreach { requiredInactivity =>
					deleteAfter.finite.foreach { deleteAfter =>
						// Case: Inactivity required =>
						// Finds issue variants that have not occurred within the specified duration
						if (requiredInactivity > Duration.Zero) {
							// FIXME: Sql syntax error here
							NotEmpty(DbIssueVariants.contextual.withSeverityIn(severities)
								.findNotOccurredSince(now - requiredInactivity))
								.foreach { targetedVariants =>
									// Deletes old occurrences of those variants
									DbIssueOccurrences.ofVariants(targetedVariants.map { _.id }.toSet)
										.before(now - deleteAfter)
										.delete()
								}
						}
						// Case: No inactivity required => Deletes old occurrences
						else
							DbManyIssueInstances.withSeverityIn(severities).before(now - deleteAfter)
								.deleteOccurrences()
					}
				}
			}
		}
		
		// Also deletes any issue variant and issue that has no recorded occurrences in the database
		DbIssueVariants.deleteNotLinkedTo(ScribeTables.issueOccurrence)
		DbIssues.deleteNotLinkedTo(ScribeTables.issueVariant)
	}
	
	// Returns Left in cases where merging was not necessary (single occurrence)
	//         Right contains merged data, plus occurrence ids that may now be deleted
	private def mergeDataFrom(occurrences: Seq[IssueOccurrence]) = {
		// If there was only one occurrence (Left), won't modify it
		occurrences.oneOrMany.mapRight { occurrences =>
			val ordered = occurrences.sorted
			val firstTime = occurrences.map { _.firstOccurrence }.min
			val lastTime = ordered.last.lastOccurrence
			val count = occurrences.map { _.count }.sum
			// Uses the latest non-empty set of error messages
			val errorMessages = ordered.reverseIterator
				.findMap { o => NotEmpty(o.errorMessages) }
				.getOrElse(Vector.empty)
			// Uses only the latest non-empty set of details
			val details = ordered.reverseIterator
				.findMap { _.details.notEmpty }
				.getOrElse(Model.empty)
			// Collects the merged occurrence (data) and occurrence ids to delete
			IssueOccurrenceData(occurrences.head.caseId, errorMessages,
				details, count, Span(firstTime, lastTime)) ->
				occurrences.map { _.id }.toSet
		}
	}
}

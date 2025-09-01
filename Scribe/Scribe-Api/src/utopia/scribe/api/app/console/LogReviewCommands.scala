package utopia.scribe.api.app.console

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.Span
import utopia.flow.collection.immutable.{Empty, IntSet, OptimizedIndexedSeq, Pair}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.mutable.DataType.{DurationType, InstantType, IntType, StringType}
import utopia.flow.parse.string.Regex
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Now, Today}
import utopia.flow.util.StringExtensions._
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.console.{ArgumentSchema, Command}
import utopia.flow.util.logging.Logger
import utopia.flow.util.{StringUtils, Version}
import utopia.flow.view.immutable.caching.Lazy
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Flag
import utopia.scribe.api.app.console.LogReviewCommands.{MoreQueue, StatusRow}
import utopia.scribe.api.database.ScribeAccessExtensions._
import utopia.scribe.api.database.access.logging.error.AccessErrorRecord
import utopia.scribe.api.database.access.logging.issue.occurrence.AccessIssueOccurrences
import utopia.scribe.api.database.access.logging.issue.variant.AccessIssueVariants
import utopia.scribe.api.database.access.logging.issue.{AccessIssue, AccessIssues}
import utopia.scribe.api.database.access.management.aliasing.AccessIssueAliases
import utopia.scribe.api.database.access.management.comment.AccessComments
import utopia.scribe.api.database.access.management.notification.AccessIssueNotifications
import utopia.scribe.api.database.access.management.resolution.AccessResolutions
import utopia.scribe.api.util.ScribeContext._
import utopia.scribe.core.model.combined.logging.{IssueVariantInstances, ManagedIssue, ManagedIssueInstances}
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.partial.logging.IssueOccurrenceData
import utopia.scribe.core.model.stored.logging.StackTraceElementRecord
import utopia.scribe.core.model.stored.management.Comment
import utopia.vault.database.Connection

import java.time.Instant
import java.time.format.DateTimeFormatter
import scala.io.StdIn
import scala.util.{Failure, Success}

object LogReviewCommands
{
	// NESTED   ------------------------------
	
	private case class StatusRow(issue: ManagedIssue, newVariantCount: Int, occurrenceCount: Int,
	                             lastOccurrence: Instant)
	
	private object MoreQueue
	{
		lazy val empty = apply()
	}
	private case class MoreQueue(status: Seq[StatusRow] = Empty, list: Seq[ManagedIssueInstances] = Empty,
	                             occurrences: Seq[IssueOccurrenceData] = Empty, timeThreshold: Option[Instant] = None)
	{
		lazy val size = status.size max list.size max occurrences.size
	}
}

/**
 * Provides console commands for reviewing log entries
 *
 * @author Mikko Hilpinen
 * @since 27.08.2025, v1.1
 */
class LogReviewCommands(implicit log: Logger)
{
	// ATTRIBUTES   --------------------------
	
	private lazy val listSeparatorRegex = Regex.anyOf(",;.+-& ")
	private lazy val contextSplitRegex = Regex.anyOf(".- /:")
	
	private lazy val otherYearDateFormat = DateTimeFormatter.ofPattern("dd.MM.YYYY")
	private lazy val otherMonthDateFormat = DateTimeFormatter.ofPattern("dd.MM")
	private lazy val otherDayDateFormat = DateTimeFormatter.ofPattern("dd HH:mm")
	private lazy val recentDayDateFormat = DateTimeFormatter.ofPattern("HH:mm")
	private lazy val sameDayDateFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
	
	// Tracks program state in separate pointers
	
	private val openIssueIdP = Pointer.eventful.empty[Int]
	private val queuedIssuesP = Pointer[(Seq[Either[Int, ManagedIssue]], Int)](Empty -> 0)
	private val queuedVariantsP = EventfulPointer[(Seq[IssueVariantInstances], Int)](Empty -> 0)
	private val queuedErrorIdP = EventfulPointer.empty[Int]
	
	// More issues and more occurrences are mutually exclusive
	private val moreP = EventfulPointer[(MoreQueue, Int)](MoreQueue.empty -> 0)
	
	/**
	 * A pointer that contains the IDs of the notifications that may be closed
	 */
	private val queuedNotificationIdsP = Pointer.eventful(IntSet.empty)
	private lazy val mayCloseNotificationsFlag: Flag = queuedNotificationIdsP.lightMap { _.nonEmpty }
	
	private val queuedCommentsP = Pointer.eventful.emptySeq[Comment]
	private val hasQueuedCommentsFlag: Flag = queuedCommentsP.lightMap { _.nonEmpty }
	
	private lazy val hasMoreVariantsFlag: Flag = queuedVariantsP
		.map { case (variants, nextIndex) => variants.hasSize > nextIndex }
	private lazy val hasQueuedErrorFlag: Flag = queuedErrorIdP.lightMap { _.isDefined }
	private lazy val hasMoreIssuesOrOccurrencesFlag: Flag = moreP
		.map { case (data, nextIndex) => data.size > nextIndex }
	
	/**
	 * A console command for summarizing active and recent issues.
	 */
	lazy val status = Command("status", "st", "Shows current issue status")(
		ArgumentSchema("since", "t", help = "A time threshold for including issues. Default = last 7 days.")) {
		args =>
			connectionPool.logging { implicit c =>
				val timeThreshold = args("since").castTo(InstantType, DurationType) match {
					case Left(timeV) => timeV.instantOr(recent)
					case Right(durationV) => Now - durationV.getDuration
				}
				println(s"Looking up status since ${ (Now - timeThreshold).description }")
				
				// Pulls active issues (number of occurrences & last occurrence time)
				val issueStatistics = AccessIssueOccurrences.since(timeThreshold, includePartialRanges = true)
					.joinIssueVariants.totalCountAndLatestTimePerIssue
				val recentIssues = AccessIssues.managed(issueStatistics.keys).pull
				
				// Looks version information for conditionally silenced issues
				val versionRequiredForIssueIds = recentIssues.view.filter { _.isConditionallySilenced }
					.map { _.id }.toIntSet
				val activeIssues = {
					if (versionRequiredForIssueIds.isEmpty)
						recentIssues.filterNot { _.isAlwaysSilenced }
					else {
						val versions = AccessIssueVariants.ofIssues(versionRequiredForIssueIds).latestVersionPerIssue
						recentIssues
							.filterNot { i => i.isSilencedInVersion(Lazy { versions.getOrElse(i.id, Version(0, 1)) }) }
					}
				}
				
				// Case: No active issues
				if (activeIssues.isEmpty)
					println("No active issues within the last 7 days")
				// Case: Active issues => Lists number of active issues by severity and handles new issues
				else {
					// Counts new issue variants
					val newVariantCounts = AccessIssueVariants.since(timeThreshold)
						.ofLimitedIssues(activeIssues.view.map { _.id }, 10).issueIds.pull.countAll
					
					// Generates a table of the active issues
					// [ ID | Severity | Name | Appeared | Notice ]
					val sortedIssues = activeIssues.reverseSortedWith(
						Ordering.by { _.hasUnreadNotifications },
						Ordering.by { _.created >= timeThreshold },
						Ordering.by { i => newVariantCounts(i.id) > 0 },
						Ordering.by { _.severity },
						Ordering.by { i => issueStatistics(i.id)._2 })
						.map { i =>
							val (occurrences, lastOccurrence) = issueStatistics(i.id)
							StatusRow(i, newVariantCounts(i.id), occurrences, lastOccurrence)
						}
					println()
					printStatusTableFor(sortedIssues.take(20), timeThreshold)
					
					// Lists a summary: [ Severity | Active | New | New variants ]
					println()
					println(StringUtils.asciiTableFrom[(Severity, Seq[ManagedIssue])](
						activeIssues.groupBy { _.severity }.toOptimizedSeq.reverseSortBy { _._1 },
						Vector("Severity", "#Active", "#New", "#New variants"),
						_._1.toString, _._2.size.toString, _._2.count { _.created >= timeThreshold }.toString,
						_._2.iterator.map { i => newVariantCounts(i.id) }.sum.toString))
					
					queuedIssuesP.value =
						sortedIssues.map[Either[Int, ManagedIssue]] { row => Right(row.issue) } -> 0
					println("\nFor more information concerning these issues, use 'see next' or 'see <issue id>'")
					
					moreP.value = MoreQueue(status = sortedIssues, timeThreshold = Some(timeThreshold)) -> 20
					if (sortedIssues.hasSize > 20)
						println("In order to list more issues, use the 'more' command")
				}
			}
	}
	/**
	 * A console command for listing currently active issues.
	 * Accepts time and context filters.
	 */
	lazy val list = Command("list", "ls", "Lists currently active issues")(
		ArgumentSchema("level", "lvl",
			help = "The level of issues to include [1,6] where 1 represents debug information and 6 represents critical failures. \nAlternatively you may use level names: Debug | Info | Warning | Recoverable | Unrecoverable | Critical. \nYou may also specify two values (lowest - highest), if you want to target a range."),
		ArgumentSchema("filter", "f", help = "A filter applied to issue context (optional)"),
		ArgumentSchema("since", "t", 7.days,
			help = "The duration or time since which issues should be scanned for. E.g. \"3d\", \"3 days\", \"2h\", \"2 hours\", \"2000-09-30\", \"2000-09-30T13:24:00\" or \"13:24\". Default = 7 days."),
		ArgumentSchema.flag("silenced", "S", help = "Whether to include silenced issues")) { args =>
		// Clears the previous state
		closeIssue()
		
		// Parses the arguments
		val since = args("since").castTo(InstantType, DurationType) match {
			case Left(instantV) => instantV.getInstant
			case Right(durationV) => Now - durationV.getDuration
		}
		val severityRange = {
			val value = args("level")
			if (value.isEmpty)
				Severity.valuesRange
			else {
				val str = value.getString
				if (str.contains('-')) {
					val ends = str.splitAtFirst("-").map { _.trim }.map { s =>
						s.int match {
							case Some(level) => Severity.forLevel(level)
							case None => Severity.forName(s)
						}
					}
					Span(ends)
				}
				else
					Span.singleValue(Severity.fromValue(value))
			}
		}
		connectionPool.tryWith { implicit c =>
			// Finds the issues
			val issues = AccessIssues.instances
				.withSeverityIn(severityRange).includingContext(args("filter").getString)
				.whereOccurrences.since(since, includePartialRanges = true)
				.pull
			
			if (issues.isEmpty)
				Empty
			else {
				// Includes management information
				val resolutions = AccessResolutions.detailed.active.ofIssues(issues.view.map { _.id })
					.pull.groupBy { _.resolvedIssueId }.withDefaultValue(Empty)
				
				// Filters out silenced issues (optional)
				val includeSilenced = args("silenced").getBoolean
				val issuesToDisplay = {
					if (includeSilenced)
						issues.map { i => i -> resolutions(i.id) }
					else
						issues.view.map { i => i -> resolutions(i.id) }
							.filterNot { case (issue, resolutions) =>
								resolutions.exists { _.silencesVersion(issue.latestVersion.getOrElse(Version(0, 1))) }
							}
							.toOptimizedSeq
				}
				
				// Loads aliases
				val aliases = AccessIssueAliases.ofIssues(issuesToDisplay.view.map { _._1.id }).toMapBy { _.issueId }
				
				// Combines info and sorts the issues
				issuesToDisplay
					.map { case (issue, resolutions) =>
						ManagedIssueInstances(issue, aliases.get(issue.id), resolutions)
					}
					.reverseSorted
			}
		} match {
			case Success(issues) =>
				// Writes a summary / list of the issues
				if (issues.isEmpty)
					println("No issues were found")
				else {
					println(s"Found ${issues.size} issues:")
					issues.iterator.take(15).foreach { summarize(_, since) }
					
					if (issues.hasSize > 15) {
						moreP.value = MoreQueue(list = issues, timeThreshold = Some(since)) -> 15
						println("\t- ...")
						println("\nFor an expanded list, use the 'more' command")
					}
					else
						println()
					
					// Writes instructions
					println("For more information concerning these issues, use 'see next' or 'see <issue id>'")
				}
			case Failure(error) =>
				log(error, "Failed tor read issues")
				println("Failed to read recent issues")
		}
	}
	/**
	 * A console command for queueing issues for the "see next" command.
	 */
	lazy val queue = Command("queue", "q",
		help = "Queues a list of issues, so that they can be unqueued with the 'see next' command")(
		ArgumentSchema("issues", help = "IDs of the issues to queue. Separated with commas, semicolons or plus signs."),
		ArgumentSchema.flag("append", "A", help = "Whether to append these issues to the queue instead of overwriting the queue.")) {
		args =>
			val input = args("issues").getString.nonEmptyOrElse(StdIn.read("Specify the issue IDs to queue").getString)
			val idList = input.splitIterator(listSeparatorRegex).flatMap { _.trim.int }.toOptimizedSeq
			if (idList.isEmpty)
				println(s"No issue IDs could be identified from input: \"$input\"")
			else {
				if (args("append").getBoolean) {
					val queueLength = queuedIssuesP.mutate { case (queue, readCount) =>
						val newQueue = (queue.view.drop(readCount) ++ idList.map[Either[Int, ManagedIssue]](Left.apply))
							.toOptimizedSeq
						newQueue.size -> (newQueue -> 0)
					}
					println(s"Added ${ idList.size } issues to the queue. The queue is now $queueLength issues long")
				}
				else {
					queuedIssuesP.value = idList.map[Either[Int, ManagedIssue]](Left.apply) -> 0
					println(s"Queued ${ idList.size } issues")
				}
				println("Unqueue issues with the \"see next\" command")
			}
	}
	/**
	 * A command for reviewing detailed information about an issue
	 */
	lazy val see = Command("see", help = "Displays more detailed information about the targeted issue")(
		ArgumentSchema("target", "t", help = "Id of the targeted issue. Alternatively 'next'")) { args =>
		// Finds the issue targeting data from user input
		val target = args("target").castTo(IntType, StringType) match {
			case Left(intV) =>
				intV.int match {
					// Case: Target specified as issue id
					case Some(issueId) => Some(Left(issueId))
					// Case: No target specified => Warns
					case None =>
						println("No issue targeted. Please specify either the targeted issue id or 'next' as a command parameter")
						None
				}
			case Right(stringV) =>
				stringV.getString.toLowerCase match {
					// Case: Target specified as 'next'
					case "next" => queuedIssuesP.mutate { case (issues, nextIndex) =>
						if (issues.hasSize > nextIndex)
							Some(issues(nextIndex)) -> (issues -> (nextIndex + 1))
						else
							None -> (issues -> nextIndex)
					}
					// Case: Invalid target
					case s => println(s"'$s' is not recognized as an issue"); None
				}
		}
		target.foreach { target =>
			connectionPool.logging { implicit c =>
				// Fetches the base issue data
				val issue = target match {
					case Left(issueId) => AccessIssue.managed(issueId).pull
					case Right(issue) => Some(issue)
				}
				issue match {
					case Some(issue) =>
						openIssueIdP.value = Some(issue.id)
						describeIssue(issue)
					// Case: No issue targeted
					case None => println("No issue was found")
				}
			}
		}
	}
	/**
	 * A console command for reviewing information about the next queued issue variant
	 */
	lazy val next = Command.withoutArguments("next", "variant",
		help = "Lists information about the next queued issue variant") {
		queuedVariantsP.mutate { case (variants, nextIndex) =>
			variants.lift(nextIndex).map { _ -> (nextIndex < variants.size - 1) } -> (variants -> (nextIndex + 1))
		} match {
			// Case: Variant found
			case Some((variant, hasMore)) =>
				// Groups similar consecutive variant issues together (based on messages and details)
				val occurrences = variant.occurrences.reverseSorted.iterator
					.groupBy { o => o.errorMessages -> o.details }
					.map { case ((_, _), group) =>
						if (group.hasSize > 1) {
							val lastItem = group.head
							val otherItems = group.tail
							lastItem.data.copy(
								occurrencePeriod = lastItem.occurrencePeriod.withStart(otherItems.last.firstOccurrence),
								count = lastItem.count + otherItems.map { _.count }.sum
							)
						}
						else
							group.head.data
					}
					.toVector
				
				println(s"Variant of issue ${variant.issueId}")
				println(s"\t- Version: ${variant.version}")
				variant.details.notEmpty.foreach { details =>
					println(s"\t- Variant details:")
					details.properties.foreach { detail =>
						println(s"\t\t- ${detail.name.capitalize}: ${detail.value}")
					}
				}
				println(s"\t- First appeared at ${timeDescription(variant.created)}")
				occurrences.headOption.foreach { last =>
					println("\t- Latest occurrence:")
					describeOccurrence(last)
					if (variant.occurrences.hasSize > 1) {
						println(s"\t- Recently there have been ${ variant.numberOfOccurrences } occurrences")
						val earliest = variant.earliestOccurrence.get.firstOccurrence
						val latest = variant.latestOccurrence.get.lastOccurrence
						println(s"\t\t- Which averages once in every ${
							((latest - earliest) / variant.numberOfOccurrences).description}")
					}
					else
						println("\t\t- This is the only recent appearance of this variant")
				}
				queuedErrorIdP.value = variant.errorId
				if (variant.errorId.isDefined)
					println("\nFor more information about the associated error, use the 'stack' command")
				moreP.value = MoreQueue(occurrences = occurrences) -> 1
				if (hasMoreIssuesOrOccurrencesFlag.value)
					println("For information about previous issue occurrences, use the 'more' command")
				if (hasMore)
					println("For information about the next variant of this issue, use the 'next' command")
			
			// Case: No more variants found
			case None => println("No more variants have been queued")
		}
	}
	/**
	 * A console command for reviewing the next issue occurrences, or for listing more issues
	 */
	lazy val more = Command.withoutArguments("more",
		help = "Prints information about another set of queued issues or occurrences") {
		// Collects the next 15 issues or the next occurrence
		val next = moreP.mutate { case (from, nextIndex) =>
			if (from.status.nonEmpty) {
				val stop = nextIndex + 20
				Right(Right(from.status.slice(nextIndex, stop))) -> (from -> stop)
			}
			else if (from.list.nonEmpty) {
				val stop = nextIndex + 15
				Right(Left(from.list.slice(nextIndex, stop))) -> (from -> stop)
			}
			else
				Left(from.occurrences.lift(nextIndex)) -> (from -> (nextIndex + 1))
		}
		next match {
			case Left(occurrence) =>
				occurrence match {
					case Some(occurrence) =>
						println(s"\tOccurrence of issue variant ${ occurrence.caseId }:")
						describeOccurrence(occurrence)
						if (hasMoreIssuesOrOccurrencesFlag.value)
							println("For information about the next set of occurrences, use this same command")
					case None => println("No more occurrences have been queued")
				}
			case Right(issues) =>
				closeIssue()
				val timeThreshold = moreP.value._1.timeThreshold.getOrElse(recent)
				issues match {
					case Left(issuesToList) => issuesToList.foreach { summarize(_, timeThreshold) }
					case Right(statusIssues) => printStatusTableFor(statusIssues, timeThreshold)
				}
				
				println()
				if (hasMoreIssuesOrOccurrencesFlag.value)
					println("For more issues, use the 'more' command")
				println("You may acquire more specifics of any single issue using the 'see <issue id>' command")
		}
	}
	/**
	 * A console command for displaying an error's stack trace
	 */
	lazy val stack = Command.withoutArguments("stack",
		help = "Prints the stack trace of the most recently encountered error") {
		queuedErrorIdP.value match {
			case Some(errorId) =>
				connectionPool.logging { implicit c =>
					// Reads and prints error data
					AccessErrorRecord(errorId).topToBottomIterator.zipWithIndex.foreach { case (error, index) =>
						// Prints one line for the error type
						val prefix = if (index == 0) "" else "Caused by: "
						println(s"$prefix${error.exceptionType}")
						// Groups by file, class and method
						error.stackAccess.topToBottomIterator.groupBy { _.fileName }
							.foreach { case (fileName, stack) =>
								stack.iterator.groupBy { _.className }.toVector.oneOrMany match {
									// Case: Only one class in this file
									case Left((_, classLines)) =>
										printClassStack(classLines, stack.head.fileAndClassName)
									// Case: Multiple classes in this file => Prints a separate header for the file
									case Right(classes) =>
										println(s"\t$fileName")
										classes.foreach { case (className, classLines) =>
											printClassStack(classLines, className.nonEmptyOrElse(fileName), 2)
										}
								}
							}
					}
				}
			case None => println("There is no error to describe")
		}
	}
	/**
	 * A command for displaying the comments on the last opened issue
	 */
	lazy val comments = Command.withoutArguments("comments", help = "Displays the comments on the last opened issue") {
		val comments = queuedCommentsP.value
		if (comments.nonEmpty)
			println(StringUtils.asciiTableFrom[Comment](comments, Pair("Time", "Comment"),
				c => timeDescription(c.created), _.text.splitToLinesIterator(80).mkString("\n")))
	}
	/**
	 * A command for marking notifications as read
	 */
	lazy val close = Command.withoutArguments("close", "read", help = "Marks the last notifications as read") {
		val idsToClose = queuedNotificationIdsP.getAndSet(IntSet.empty)
		if (idsToClose.nonEmpty) {
			connectionPool.logging { implicit c =>
				AccessIssueNotifications(idsToClose).deprecate()
				println(s"Marked ${ idsToClose.size } notifications as read")
			}
		}
	}
	
	// Determines the commands available at each time
	private lazy val staticCommands = Vector(status, list, queue, see)
	/**
	 * A pointer that contains the currently available commands
	 */
	lazy val pointer = hasMoreVariantsFlag
		.mergeWith(Vector(hasQueuedErrorFlag, hasMoreIssuesOrOccurrencesFlag, hasQueuedCommentsFlag,
			mayCloseNotificationsFlag)) {
			variants =>
				val builder = OptimizedIndexedSeq.newBuilder[Command]
				if (hasQueuedCommentsFlag.value)
					builder += comments
				if (displayedNotifications)
					builder += close
				if (hasQueuedErrors)
					builder += stack
				if (hasMoreIssuesOrOccurrences)
					builder += this.more
				if (variants)
					builder += next
				staticCommands ++ builder.result()
		}
	
	/**
	 * A pointer that contains the ID of the currently viewed issue, if applicable
	 */
	lazy val openIssueIdPointer = openIssueIdP.readOnly
	
	
	// COMPUTED ---------------------------------
	
	/**
	 * @return The currently available log review console commands
	 * @see [[pointer]]
	 */
	def currently = pointer.value
	
	def hasQueuedErrors = hasQueuedErrorFlag.value
	def hasMoreIssuesOrOccurrences = hasMoreIssuesOrOccurrencesFlag.value
	def displayedNotifications = mayCloseNotificationsFlag.value
	
	private def recent = Now - 7.days
	
	
	// OTHER    ---------------------------------
	
	/**
	 * Closes pointers related to the currently open issue.
	 * Should be called when listing new issues.
	 */
	private def closeIssue() = {
		openIssueIdP.clear()
		queuedVariantsP.value = Empty -> 0
		queuedErrorIdP.clear()
		moreP.update { case (queue, nextIndex) =>
			if (queue.occurrences.nonEmpty)
				MoreQueue.empty -> 0
			else
				queue -> nextIndex
		}
		queuedNotificationIdsP.value = IntSet.empty
		queuedCommentsP.clear()
	}
	
	private def printStatusTableFor(issues: Seq[StatusRow], timeThreshold: Instant = recent) =
		println(StringUtils.asciiTableFrom[StatusRow](issues,
			Vector("ID", "Severity", "Context", "When", "Count", "Notice"),
			_.issue.id.toString, _.issue.severity.toString,
			_.issue.aliasOrContext.splitToLinesIterator(40, contextSplitRegex).mkString("\n"),
			i => timeDescription(i.lastOccurrence), _.occurrenceCount.toString,
			{ row =>
				if (row.issue.hasUnreadNotifications)
					"!!!"
				else if (row.issue.created >= timeThreshold)
					"NEW"
				else {
					if (row.newVariantCount == 1)
						"New variant"
					else if (row.newVariantCount > 0)
						s"${ row.newVariantCount } new variants"
					else
						""
				}
			}))
	
	private def summarize(issue: ManagedIssueInstances, threshold: Instant = recent) = {
		val state = {
			if (issue.created >= threshold)
				" - NEW"
			else if (issue.variants.exists { _.created >= threshold })
				" - new variants"
			else
				""
		}
		println(s"\t- ${ issue.id } ${ issue.severity } ${ issue.aliasOrContext }$state")
		issue.latestOccurrence.foreach { latest =>
			latest.errorMessages.headOption.foreach { message => println(s"\t\t- $message") }
			val lastTime = latest.lastOccurrence
			println(s"\t\t- Last occurred ${(Now - lastTime).description} ago")
			val earliest = issue.earliestOccurrence match {
				case Some(o) => o.firstOccurrence
				case None => latest.firstOccurrence
			}
			if (earliest == lastTime)
				println("\t\t- Has occurred only once so far")
			else
				println(s"\t\t- Has occurred ${issue.numberOfOccurrences} times over ${(lastTime - earliest).description}")
		}
	}
	
	private def describeIssue(issue: ManagedIssue)(implicit connection: Connection) = {
		// Loads information about this issue's variants and occurrences
		val variants = AccessIssueVariants.instances.ofIssue(issue.id).pull
		val lastVersionVariants = variants.filterMaxBy { _.version }
		
		val lastOccurrence = variants.iterator.flatMap { _.occurrences }.maxByOption { _.lastOccurrence }
		
		val numberOfOccurrences = variants.iterator.map { _.numberOfOccurrences }.sum
		val averageOccurrenceInterval = {
			if (numberOfOccurrences > 1)
				Some((Now - issue.created) / numberOfOccurrences)
			else
				None
		}
		val affectedVersions = variants.iterator.map { _.version }.minMaxOption
		
		issue.alias.ifNotEmpty match {
			case Some(alias) =>
				println(s"${ issue.id }: $alias (${ issue.severity })")
				println(s"\t- Context: ${ issue.context }")
			
			case None => println(s"${issue.id}: ${issue.severity} @ ${issue.context}")
		}
		println(s"\t- First encountered ${ timeDescription(issue.created) }")
		lastOccurrence.foreach { last =>
			println("\t- Last occurrence:")
			describeOccurrence(last)
		}
		println(s"\t- $numberOfOccurrences occurrences in total")
		averageOccurrenceInterval.foreach { interval =>
			println(s"\t- Has occurred once every ${interval.description}")
		}
		
		lastVersionVariants.iterator.flatMap { _.occurrences }.map { _.firstOccurrence }.minOption
			.foreach { firstOccurrence =>
				val lastOccurrence = lastVersionVariants.iterator.flatMap { _.occurrences }.map { _.lastOccurrence }.max
				if (firstOccurrence != lastOccurrence) {
					val averageInterval = (lastOccurrence - firstOccurrence) /
						lastVersionVariants.iterator.map { _.numberOfOccurrences }.sum
					val threshold = Now - 24.hours
					val recentOccurrences = lastVersionVariants.iterator.flatMap { _.occurrences }
						.map { _.countSince(threshold) }.sum
					
					println(s"\t\t- In version ${ lastVersionVariants.head.version }, once every ${
						averageInterval.description }; ${ recentOccurrences.round } times within the last 24 hours")
				}
			}
		
		affectedVersions.foreach { versions =>
			if (versions.isSymmetric)
				println(s"\t- Affects version: ${ versions.first }")
			else
				println(s"\t- Affects versions: [${ versions.mkString(" - ") }]")
		}
		if (variants.isEmpty)
			println("\t- Has not appeared recently")
		else {
			val threshold = recent
			println(s"\t- Has ${ variants.size } different variants, ${
				variants.count { _.occurrences.exists { _.lastOccurrence >= threshold } } } are active")
		}
		
		// Looks up comments
		val comments = AccessComments.onIssue(issue.id).pull.sortBy { _.created }
		queuedCommentsP.value = comments
		if (comments.nonEmpty)
			println(s"\t- ${ comments.size } comments")
		
		// Checks information about notifications and/or resolutions
		val notifications = issue.unreadNotifications.reverseSortBy { _._2.created }
		notifications.foreach { case (resolution, notification) =>
			resolution.versionThreshold match {
				case Some(fixVersion) =>
					println(s"\t- Was marked as fixed in $fixVersion ${
						timeDescription(resolution.created) }, but reappeared ${
						timeDescription(notification.created) } (${
						(notification.created - resolution.created).description } later)")
				case None =>
					println(s"\t- Was supposed to be fixed ${ timeDescription(resolution.created) }, but reappeared ${
						timeDescription(notification.created) } (${
						(notification.created - resolution.created).description } later)")
			}
			resolution.text.ifNotEmpty.foreach { comment =>
				println(s"\t\t- Comment: \"$comment\"")
			}
		}
		queuedNotificationIdsP.value = IntSet.from(notifications.view.map { _._2.id })
		
		if (comments.nonEmpty || notifications.nonEmpty || variants.nonEmpty) {
			println()
			if (comments.nonEmpty)
				println("To list comments concerning this issue, use the 'comments' command")
			if (notifications.nonEmpty)
				println("To mark the notifications as read, use the 'close' command")
			if (variants.nonEmpty)
				println("For more information about each variant, use the 'next' command")
		}
		
		// Queues information about the issue variants
		queuedVariantsP.value = variants.reverseSorted -> 0
	}
	
	private def printClassStack(classLines: IterableOnce[StackTraceElementRecord], className: String, indents: Int = 1) =
	{
		val indentStr = "\t" * indents
		// Groups by method names
		classLines.iterator.groupBy { _.methodName }.toVector.oneOrMany match {
			// Case: Only one method involved => Prints class and method on one line
			case Left((methodName, lines)) =>
				println(s"$indentStr$className.$methodName${ lineNumberString(lines) }")
			// Case: Multiple methods involved => Prints class first and each method on a separate line
			case Right(methods) =>
				println(s"$indentStr$className")
				methods.foreach { case (methodName, lines) =>
					println(s"$indentStr\t$methodName${ lineNumberString(lines) }")
				}
		}
	}
	
	private def lineNumberString(stack: Iterable[StackTraceElementRecord]) =
		stack.flatMap { _.lineNumber }.oneOrMany match {
			case Left(line) => s": $line"
			case Right(lines) => if (lines.isEmpty) "" else s": [${lines.mkString(", ")}]"
		}
	
	// Describes at indentation 2
	private def describeOccurrence(occurrence: IssueOccurrenceData) = {
		if (occurrence.count > 1)
			println(s"\t\t- ${occurrence.count} occurrences, which appeared between \n\t\t${
				timeDescription(occurrence.firstOccurrence)} and \n\t\t${timeDescription(occurrence.lastOccurrence)} \n\t\t(during ${
				occurrence.duration.description})")
		else
			println(s"\t\t- Appeared ${timeDescription(occurrence.lastOccurrence)}")
		
		if (occurrence.errorMessages.hasSize > 1) {
			println(s"\t\t- \"Story\":")
			occurrence.errorMessages.reverseIterator.foreach { msg =>
				println(s"\t\t\t- $msg")
			}
		}
		else if (occurrence.errorMessages.nonEmpty)
			println(s"\t\t- Message: ${ occurrence.errorMessages.head }")
		
		if (occurrence.details.nonEmpty) {
			println("\t\t- Details:")
			occurrence.details.properties.foreach { detail =>
				println(s"\t\t\t- ${detail.name.capitalize}: ${detail.value}")
			}
		}
	}
	
	private def timeDescription(time: Instant) = {
		val local = time.toLocalDateTime
		val localDate = local.toLocalDate
		val today = Today.toLocalDate
		val base = {
			if (localDate == today)
				s"today at ${local.format(sameDayDateFormat)}"
			else if (localDate == today.yesterday)
				s"yesterday at ${local.format(recentDayDateFormat)}"
			else if (today - localDate < 7.days)
				s"last ${localDate.weekDay} at ${local.format(recentDayDateFormat)}"
			else if (localDate.monthOfYear == today.monthOfYear)
				local.format(otherDayDateFormat)
			else if (localDate.year == today.year)
				local.format(otherMonthDateFormat)
			else
				local.format(otherYearDateFormat)
		}
		s"$base (${ (Now - time).description } ago)"
	}
}

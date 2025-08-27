package utopia.scribe.api.app.console

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.range.Span
import utopia.flow.collection.immutable.{Empty, OptimizedIndexedSeq}
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.mutable.DataType.{DurationType, InstantType, IntType, StringType}
import utopia.flow.parse.string.Regex
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Now, Today}
import utopia.flow.util.EitherExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.console.ConsoleExtensions._
import utopia.flow.util.console.{ArgumentSchema, Command}
import utopia.flow.util.logging.Logger
import utopia.flow.util.{StringUtils, Version}
import utopia.flow.view.immutable.View
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.flow.view.template.eventful.Flag
import utopia.scribe.api.database.ScribeAccessExtensions._
import utopia.scribe.api.database.access.logging.error.AccessErrorRecord
import utopia.scribe.api.database.access.logging.issue.occurrence.AccessIssueOccurrences
import utopia.scribe.api.database.access.logging.issue.variant.AccessIssueVariants
import utopia.scribe.api.database.access.logging.issue.{AccessIssue, AccessIssues}
import utopia.scribe.api.database.access.management.aliasing.AccessIssueAliases
import utopia.scribe.api.database.access.management.resolution.AccessResolutions
import utopia.scribe.api.util.ScribeContext._
import utopia.scribe.core.model.combined.logging.{IssueInstances, IssueVariantInstances, ManagedIssue, ManagedIssueInstances}
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.partial.logging.IssueOccurrenceData
import utopia.scribe.core.model.stored.logging.StackTraceElementRecord
import utopia.vault.database.Connection

import java.time.Instant
import java.time.format.DateTimeFormatter
import scala.io.StdIn
import scala.util.{Failure, Success}

/**
 * Provides console commands for reviewing log entries
 *
 * @author Mikko Hilpinen
 * @since 27.08.2025, v1.1
 */
class LogReviewCommands(lazyTargetAppVersion: View[Version])(implicit log: Logger)
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
	
	// TODO: Possibly include ManagedIssue as an option
	private val queuedIssuesP = Pointer[(Seq[Either[Int, IssueInstances]], Int)](Empty -> 0)
	private val queuedVariantsP = EventfulPointer[(Seq[IssueVariantInstances], Int)](Empty -> 0)
	private val queuedErrorIdP = EventfulPointer.empty[Int]
	
	// More issues and more occurrences are mutually exclusive
	private val moreIssuesOrOccurrencesP =
		EventfulPointer[(Either[Seq[IssueOccurrenceData], Seq[ManagedIssueInstances]], Int)](Left(Empty) -> 0)
	
	private lazy val hasMoreVariantsFlag: Flag = queuedVariantsP
		.map { case (variants, nextIndex) => nextIndex < variants.size }
	private lazy val hasQueuedErrorFlag: Flag = queuedErrorIdP.map { _.isDefined }
	private lazy val hasMoreIssuesOrOccurrencesFlag: Flag = moreIssuesOrOccurrencesP
		.map { case (data, nextIndex) => data.either.hasSize > nextIndex }
	
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
				
				// Pulls active issues
				val activeIssues = AccessIssues.managed.active
					.whereOccurrences.since(timeThreshold, includePartialRanges = true).pull
					// Skips those that have been silenced
					.filterNot { _.isSilencedInVersion(lazyTargetAppVersion) }
				
				// Case: No active issues
				if (activeIssues.isEmpty)
					println("No active issues within the last 7 days")
				// Case: Active issues => Lists number of active issues by severity and handles new issues
				else {
					// Counts new issue variants
					val newVariantCounts = AccessIssueVariants.since(timeThreshold)
						.ofLimitedIssues(activeIssues.view.map { _.id }, 10).issueIds.pull.countAll
					
					// Lists a summary: [ Severity | Active | New | New variants ]
					println(StringUtils.asciiTableFrom[(Severity, Seq[ManagedIssue])](
						activeIssues.groupBy { _.severity }.toOptimizedSeq.reverseSortBy { _._1 },
						Vector("Severity", "#Active", "#New", "#New variants"),
						_._1.toString, _._2.size.toString, _._2.count { _.created >= timeThreshold }.toString,
						_._2.iterator.map { i => newVariantCounts(i.id) }.sum.toString))
					
					// Generates a table of the active issues
					// [ ID | Severity | Name | Appeared | Notice ]
					val sortedIssues = activeIssues.reverseSortedWith(
						Ordering.by { _.hasUnreadNotifications },
						Ordering.by { _.created >= timeThreshold },
						Ordering.by { i => newVariantCounts(i.id) > 0 },
						Ordering.by { _.severity },
						Ordering.by { _.created })
					println()
					println(StringUtils.asciiTableFrom[ManagedIssue](sortedIssues,
						Vector("ID", "Severity", "Context", "Appeared", "Notice"),
						_.id.toString, _.severity.toString,
						_.aliasOrContext.splitToLinesIterator(60, contextSplitRegex).mkString("\n"),
						i => (Now - i.created).description),
						{ i: ManagedIssue =>
							if (i.hasUnreadNotifications)
								"!!!"
							else if (i.created >= timeThreshold)
								"NEW"
							else {
								val newVariantsCount = newVariantCounts(i.id)
								if (newVariantsCount == 1)
									"New variant"
								else if (newVariantsCount > 0)
									s"$newVariantsCount new variants"
								else
									""
							}
						})
					
					queuedIssuesP.value = sortedIssues.map { i => Left(i.id) } -> 0
					println("For more information concerning these issues, use 'see next' or 'see <issue id>'")
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
		ArgumentSchema("since", "t", 7.days, help = "The duration or time since which issues should be scanned for. E.g. \"3d\", \"3 days\", \"2h\", \"2 hours\", \"2000-09-30\", \"2000-09-30T13:24:00\" or \"13:24\". Default = 7 days.")) { args =>
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
				
				// Filters out silenced issues
				// TODO: Make this an optional step
				val issuesToDisplay = issues.view.map { i => i -> resolutions(i.id) }
					.filterNot { _._2.exists { _.silencesVersion(lazyTargetAppVersion.value) } }
					.toOptimizedSeq
				
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
						moreIssuesOrOccurrencesP.value = Right(issues) -> 15
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
		ArgumentSchema.flag("append", "A", help = "Whether to append these issues to the queue instead of overwriting the queue")) {
		args =>
			val input = args("issues").getString.nonEmptyOrElse(StdIn.read("Specify the issue IDs to queue").getString)
			val idList = input.splitIterator(listSeparatorRegex).flatMap { _.trim.int }.toOptimizedSeq
			if (idList.isEmpty)
				println(s"No issue IDs could be identified from input: \"$input\"")
			else {
				if (args("append").getBoolean) {
					val queueLength = queuedIssuesP.mutate { case (queue, readCount) =>
						val newQueue = (queue.view.drop(readCount) ++ idList.map[Either[Int, IssueInstances]](Left.apply))
							.toOptimizedSeq
						newQueue.size -> (newQueue -> 0)
					}
					println(s"Added ${ idList.size } issues to the queue. The queue is now $queueLength issues long")
				}
				else {
					queuedIssuesP.value = idList.map[Either[Int, IssueInstances]](Left.apply) -> 0
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
					case Left(issueId) =>
						AccessIssue.withVariants(issueId).pull.map { issue =>
							issue.withOccurrences(
								AccessIssueOccurrences.ofVariants(issue.variants.view.map { _.id }).pull)
						}
					case Right(issue) => Some(issue)
				}
				issue match {
					case Some(issue) => describeIssue(issue)
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
				moreIssuesOrOccurrencesP.value = Left(occurrences) -> 1
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
		val next = moreIssuesOrOccurrencesP.mutate { case (from, nextIndex) =>
			from match {
				case Right(issues) => Right(issues.slice(nextIndex, nextIndex + 15)) -> (from -> (nextIndex + 15))
				case Left(occurrences) => Left(occurrences.lift(nextIndex)) -> (from -> (nextIndex + 1))
			}
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
				issues.foreach { summarize(_) }
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
	
	// Determines the commands available at each time
	private lazy val staticCommands = Vector(status, list, queue, see)
	/**
	 * A pointer that contains the currently available commands
	 */
	lazy val pointer = hasMoreVariantsFlag
		.mergeWith(hasQueuedErrorFlag, hasMoreIssuesOrOccurrencesFlag) { (variants, error, more) =>
			val builder = OptimizedIndexedSeq.newBuilder[Command]
			if (error)
				builder += stack
			if (more)
				builder += this.more
			if (variants)
				builder += next
			staticCommands ++ builder.result()
		}
	
	
	// COMPUTED ---------------------------------
	
	/**
	 * @return The currently available log review console commands
	 * @see [[pointer]]
	 */
	def currently = pointer.value
	
	private def recent = Now - 7.days
	
	
	// OTHER    ---------------------------------
	
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
	
	private def describeIssue(issue: IssueInstances)(implicit connection: Connection) = {
		// Retrieves and prints available information about the issue
		val lastOccurrence = issue.latestOccurrence
		val numberOfOccurrences = {
			val access = AccessIssueOccurrences.ofVariants(issue.variantIds)
			val targetedAccess = issue.earliestOccurrence match {
				case Some(o) => access.before(o.lastOccurrence)
				case None => access
			}
			targetedAccess.totalCount + issue.numberOfOccurrences
		}
		val averageOccurrenceInterval = {
			if (numberOfOccurrences > 1)
				Some((Now - issue.created) / numberOfOccurrences)
			else
				None
		}
		val affectedVersions = issue.variants.map { _.version }.minMaxOption
		
		println(s"${issue.id}: ${issue.severity} @ ${issue.context}")
		println(s"\t- First encountered ${timeDescription(issue.created)}")
		lastOccurrence.foreach { last =>
			println("\t- Last occurrence:")
			describeOccurrence(last)
		}
		println(s"\t- $numberOfOccurrences occurrences in total")
		averageOccurrenceInterval.foreach { interval =>
			println(s"\t- Has occurred once every ${interval.description}")
		}
		issue.latestOccurrence.foreach { last =>
			issue.earliestOccurrence.foreach { first =>
				val start = first.firstOccurrence
				val end = last.lastOccurrence
				if (start != end)
					println(s"\t\t- Recently once every ${
						((end - start) / issue.numberOfOccurrences).description
					}")
			}
		}
		affectedVersions.foreach { versions =>
			if (versions.isSymmetric)
				println(s"\t- Affects version: ${ versions.first }")
			else
				println(s"\t- Affects versions: [${ versions.mkString(" - ") }]")
		}
		if (issue.variants.isEmpty)
			println("\t- Has not appeared recently")
		else {
			println(s"\t- Has ${issue.variants.size} different variants")
			println("\nFor more information about each variant, use the 'next' command")
		}
		
		// Queues information about the issue variants
		queuedVariantsP.value = issue.variants -> 0
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

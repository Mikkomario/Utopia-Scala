package utopia.scribe.api.app.console

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Empty
import utopia.flow.collection.immutable.range.Span
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.mutable.DataType.{DurationType, InstantType, IntType, StringType}
import utopia.flow.operator.Identity
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.{Now, Today}
import utopia.flow.util.StringExtensions._
import utopia.flow.util.console.{ArgumentSchema, Command, Console}
import utopia.flow.util.logging.{FileLogger, Logger, SysErrLogger}
import utopia.flow.view.immutable.eventful.Fixed
import utopia.flow.view.mutable.Pointer
import utopia.flow.view.mutable.eventful.EventfulPointer
import utopia.scribe.api.database.ScribeAccessExtensions._
import utopia.scribe.api.database.access.many.logging.issue.{DbIssues, DbManyIssueInstances}
import utopia.scribe.api.database.access.many.logging.issue_occurrence.DbIssueOccurrences
import utopia.scribe.api.database.access.many.logging.issue_variant.DbIssueVariants
import utopia.scribe.api.database.access.single.logging.error_record.DbErrorRecord
import utopia.scribe.api.database.access.single.logging.issue.DbVaryingIssue
import utopia.scribe.api.util.ScribeContext
import utopia.scribe.core.model.combined.logging.{IssueInstances, IssueVariantInstances}
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.partial.logging.IssueOccurrenceData
import utopia.scribe.core.model.stored.logging.StackTraceElementRecord
import utopia.vault.database.{ConnectionPool, Tables}

import java.time.Instant
import java.time.format.DateTimeFormatter
import scala.collection.immutable.VectorBuilder
import scala.util.{Failure, Success}

/**
  * A command-line application that provides an interface for checking issue status
  * @author Mikko Hilpinen
  * @since 25.5.2023, v0.1
  */
// TODO: Split commands/actions to separate files
object ScribeConsoleApp extends App
{
	private val otherYearDateFormat = DateTimeFormatter.ofPattern("dd.MM.YYYY")
	private val otherMonthDateFormat = DateTimeFormatter.ofPattern("dd.MM")
	private val otherDayDateFormat = DateTimeFormatter.ofPattern("dd HH:mm")
	private val recentDayDateFormat = DateTimeFormatter.ofPattern("HH:mm")
	private val sameDayDateFormat = DateTimeFormatter.ofPattern("HH:mm:ss")
	
	// Initializes settings and context (may fail)
	
	ScribeConsoleSettings.initializeDbSettings().get
	
	private implicit val jsonParser: JsonParser = JsonBunny
	private implicit val exc: ThreadPool = new ThreadPool("Scribe-Console", 3, 100,
		10.seconds)(SysErrLogger)
	private implicit val log: Logger = ScribeConsoleSettings.logDirectory match {
		case Some(dir) => new FileLogger(dir, 1.seconds, copyToSysErr = true)
		case None => SysErrLogger
	}
	private implicit val cPool: ConnectionPool = new ConnectionPool(30)
	
	ScribeContext.setup(exc, cPool, new Tables(cPool), ScribeConsoleSettings.dbName, backupLogger = log)
	
	// Tracks program state in separate pointers
	
	private val queuedIssuesPointer = Pointer[(Seq[Either[Int, IssueInstances]], Int)](Empty -> 0)
	private val queuedVariantsPointer = EventfulPointer[(Seq[IssueVariantInstances], Int)](Empty -> 0)
	private val queuedErrorIdPointer = EventfulPointer.empty[Int]()
	
	// More issues and more occurrences are mutually exclusive
	private val moreIssuesOrOccurrencesPointer =
		EventfulPointer[(Either[Seq[IssueOccurrenceData], Seq[IssueInstances]], Int)](Left(Empty) -> 0)
	
	private val hasMoreVariantsPointer = queuedVariantsPointer
		.map { case (variants, nextIndex) => nextIndex < variants.size }
	private val hasQueuedErrorPointer = queuedErrorIdPointer.map { _.isDefined }
	private val hasMoreIssuesOrOccurrencesPointer = moreIssuesOrOccurrencesPointer
		.map { case (data, nextIndex) => data.either.hasSize > nextIndex }
	
	// Specifies some computed properties
	
	private def recent = Now - 7.days
	
	
	// Sets up the commands for the console
	
	// Summarizes active and new issues
	private val statusCommand = Command.withoutArguments("status", "st", "Shows current issue status") {
		cPool.tryWith { implicit c =>
			// Counts active issues
			val activeIssueIds = DbManyIssueInstances.since(recent, includePartialRanges = true).ids.toSet
			
			// Case: No active issues
			if (activeIssueIds.isEmpty)
				println("No active issues within the last 7 days")
			// Case: Active issues => Lists number of active issues by severity and handles new issues
			else {
				val countBySeverity = DbIssues(activeIssueIds).severities
					.groupMapReduce(Identity) { _ => 1 } { _ + _ }
				println(s"${ activeIssueIds.size } active issues:")
				countBySeverity.keys.toVector.reverseSorted.foreach { severity =>
					println(s"\t- $severity: ${countBySeverity(severity)} issues")
				}
				
				// Checks for new issues
				val newIssueIds = DbIssueVariants.after(recent).issueIds
					.groupMapReduce(Identity) { _ => 1 } { _ + _ }
					.toVector.reverseSorted
				
				// Case: No new issues
				if (newIssueIds.isEmpty)
					println("None of the issues are new")
				// Case: New issues => Lists their ids
				else {
					println(s"${newIssueIds.size} of the issues have appeared just recently (i.e. in last 7 days):")
					val newIssueById = DbIssues(newIssueIds.map { _._1 }.toSet).toMapBy { _.id }
					val orderedNewIssueIds = newIssueIds
						.reverseSortBy { case (issueId, _) => newIssueById.get(issueId) }
					orderedNewIssueIds.foreach { case (issueId, variantCount) =>
						val baseStr = newIssueById.get(issueId) match {
							case Some(issue) => s"\t- $issueId: ${issue.severity} @ ${issue.context}"
							case None => s"\t- $issueId"
						}
						if (variantCount > 1)
							println(s"$baseStr - $variantCount new variants")
						else
							println(baseStr)
					}
					queuedIssuesPointer.value = orderedNewIssueIds.map { case (id, _) => Left(id) } -> 0
					println("For more information concerning these issues, use 'see next' or 'see <issue id>'")
				}
			}
		}.logFailure
	}
	// Used for listing latest issues
	private val listCommand = Command("list", "ls", "Lists currently active issues")(
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
		// Finds the issues
		cPool.tryWith { implicit c =>
			val occurrencesPerVariantId = DbIssueOccurrences.since(since, includePartialRanges = true).pull
				.groupBy { _.caseId }
			if (occurrencesPerVariantId.isEmpty)
				Empty
			else {
				val variantsPerIssueId = DbIssueVariants(occurrencesPerVariantId.keySet).pull
					.map { v => v.withOccurrences(occurrencesPerVariantId(v.id).reverseSorted) }
					.groupBy { _.issueId }
				val issues = DbIssues(variantsPerIssueId.keySet)
					.withSeverityIn(severityRange).includingContext(args("filter").getString)
					.pull
					.map { issue => issue.withOccurrences(variantsPerIssueId(issue.id).reverseSorted) }.reverseSorted
				// Queues the issues, enabling more detailed checks
				queuedIssuesPointer.value = issues.map { Right(_) } -> 0
				issues
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
						moreIssuesOrOccurrencesPointer.value = Right(issues) -> 15
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
	private val seeCommand = Command("see", help = "Displays more detailed information about the targeted issue")(
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
					case "next" => queuedIssuesPointer.mutate { case (issues, nextIndex) =>
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
			cPool.tryWith { implicit c =>
				// Fetches the base issue data
				val issue = target match {
					case Left(issueId) =>
						DbVaryingIssue(issueId).pull.map { issue =>
							issue.withOccurrences(DbIssueOccurrences.ofVariants(issue.variants.map { _.id }).pull)
						}
					case Right(issue) => Some(issue)
				}
				issue match {
					case Some(issue) =>
						// Retrieves and prints available information about the issue
						val lastOccurrence = issue.latestOccurrence
						val numberOfOccurrences = {
							val access = DbIssueOccurrences.ofVariants(issue.variantIds)
							val targetedAccess = issue.earliestOccurrence match {
								case Some(o) => access.before(o.lastOccurrence)
								case None => access
							}
							targetedAccess.counts.sum + issue.numberOfOccurrences
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
						queuedVariantsPointer.value = issue.variants -> 0
					
					// Case: No issue targeted
					case None => println("No issue was found")
				}
			}.logFailure
		}
	}
	private val nextCommand = Command.withoutArguments("next", "variant",
		help = "Lists information about the next queued issue variant") {
		queuedVariantsPointer.mutate { case (variants, nextIndex) =>
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
				queuedErrorIdPointer.value = variant.errorId
				if (variant.errorId.isDefined)
					println("\nFor more information about the associated error, use the 'stack' command")
				moreIssuesOrOccurrencesPointer.value = Left(occurrences) -> 1
				if (hasMoreIssuesOrOccurrencesPointer.value)
					println("For information about previous issue occurrences, use the 'more' command")
				if (hasMore)
					println("For information about the next variant of this issue, use the 'next' command")
				
			// Case: No more variants found
			case None => println("No more variants have been queued")
		}
	}
	private val moreCommand = Command.withoutArguments("more",
		help = "Prints information about another set of queued issues or occurrences") {
		// Collects the next 15 issues or the next occurrence
		val next = moreIssuesOrOccurrencesPointer.mutate { case (from, nextIndex) =>
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
						if (hasMoreIssuesOrOccurrencesPointer.value)
							println("For information about the next set of occurrences, use this same command")
					case None => println("No more occurrences have been queued")
				}
			case Right(issues) =>
				issues.foreach { summarize(_) }
				println()
				if (hasMoreIssuesOrOccurrencesPointer.value)
					println("For more issues, use the 'more' command")
				println("You may acquire more specifics of any single issue using the 'see <issue id>' command")
		}
	}
	private val stackCommand = Command.withoutArguments("stack",
		help = "Prints the stack trace of the most recently encountered error") {
		queuedErrorIdPointer.value match {
			case Some(errorId) =>
				cPool.tryWith { implicit c =>
					// Reads and prints error data
					DbErrorRecord(errorId).topToBottomIterator.zipWithIndex.foreach { case (error, index) =>
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
				}.logFailure
			case None => println("There is no error to describe")
		}
	}
	
	// Determines the commands available at each time
	private val staticCommands = Vector(statusCommand, listCommand, seeCommand)
	private val commandsPointer = hasMoreVariantsPointer
		.mergeWith(hasQueuedErrorPointer, hasMoreIssuesOrOccurrencesPointer) { (variants, error, more) =>
			val builder = new VectorBuilder[Command]()
			if (error)
				builder += stackCommand
			if (more)
				builder += moreCommand
			if (variants)
				builder += nextCommand
			staticCommands ++ builder.result()
		}
	
	// Starts the console
	println("Welcome to the Scribe utility console!")
	println("You will find the available commands with the 'help' command.")
	Console(Fixed(staticCommands ++ Vector(nextCommand, stackCommand, moreCommand)),
		s"\nNext command (${commandsPointer.value.map { _.name }.mkString(" | ")}):", closeCommandName = "exit")
		.run()
	println("Bye!")
	
	
	// OTHER FUNCTIONS  -------------------------
	
	private def summarize(issue: IssueInstances, threshold: Instant = recent) = {
		val state = {
			if (issue.created >= threshold)
				" - NEW"
			else if (issue.variants.exists { _.created >= threshold })
				" - new variants"
			else
				""
		}
		println(s"\t- ${issue.id} ${issue.severity} ${issue.context}$state")
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

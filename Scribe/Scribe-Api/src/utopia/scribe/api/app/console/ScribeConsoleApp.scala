package utopia.scribe.api.app.console

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.mutable.DataType.{DurationType, InstantType, IntType, StringType}
import utopia.flow.operator.Identity
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.{Now, Today}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.console.{ArgumentSchema, Command, Console}
import utopia.flow.util.logging.{FileLogger, Logger, SysErrLogger}
import utopia.flow.view.mutable.Pointer
import utopia.scribe.api.database.ScribeAccessExtensions._
import utopia.scribe.api.database.access.many.logging.issue.{DbIssues, DbManyIssueInstances}
import utopia.scribe.api.database.access.many.logging.issue_occurrence.DbIssueOccurrences
import utopia.scribe.api.database.access.many.logging.issue_variant.DbIssueVariants
import utopia.scribe.api.database.access.single.logging.error_record.DbErrorRecord
import utopia.scribe.api.database.access.single.logging.issue.DbVaryingIssue
import utopia.scribe.api.util.ScribeContext
import utopia.scribe.core.model.combined.logging.{IssueInstances, IssueVariantInstances}
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.enumeration.Severity.Debug
import utopia.scribe.core.model.partial.logging.IssueOccurrenceData
import utopia.vault.database.ConnectionPool

import java.time.Instant
import java.time.format.DateTimeFormatter
import scala.util.{Failure, Success}

/**
  * A command-line application that provides an interface for checking issue status
  * @author Mikko Hilpinen
  * @since 25.5.2023, v0.1
  */
object ScribeConsoleApp extends App
{
	private val otherYearDateFormat = DateTimeFormatter.ofPattern("dd.MM.YYYY")
	private val otherMonthDateFormat = DateTimeFormatter.ofPattern("dd.MM")
	private val otherDayDateFormat = DateTimeFormatter.ofPattern("dd hh:mm")
	private val recentDayDateFormat = DateTimeFormatter.ofPattern("hh:mm")
	private val sameDayDateFormat = DateTimeFormatter.ofPattern("hh:mm:ss")
	
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
	
	ScribeContext.setup(exc, cPool, ScribeConsoleSettings.dbName, backupLogger = log)
	
	// Tracks program state in separate pointers
	
	private val queuedIssuesPointer = Pointer(Vector.empty[Either[Int, IssueInstances]] -> 0)
	private val queuedVariantsPointer = Pointer(Vector.empty[IssueVariantInstances] -> 0)
	private val queuedErrorIdPointer = Pointer.empty[Int]()
	
	private var queuedOccurrences = Iterator.empty[IssueOccurrenceData]
	
	// Specifies some computed properties
	
	private def recent = Now - 7.days
	
	// Sets up the commands for the console
	
	// Summarizes active and new issues
	private val statusCommand = Command.withoutArguments("status", "st", "Shows current issue status") {
		cPool.tryWith { implicit c =>
			// Counts active issues
			val activeIssueIds = DbManyIssueInstances.since(recent).ids.toSet
			
			// Case: No active issues
			if (activeIssueIds.isEmpty)
				println("No active issues within the last 7 days")
			// Case: Active issues => Lists number of active issues by severity and handles new issues
			else {
				val countBySeverity = DbIssues(activeIssueIds).severities
					.groupMapReduce(Identity) { _ => 1 } { _ + _ }
				println(s"${ countBySeverity.valuesIterator.sum } active issues:")
				countBySeverity.keys.toVector.reverseSorted.foreach { severity =>
					println(s"\t- $severity: ${countBySeverity(severity)} issues")
				}
				
				// Checks for new issues
				val newIssueIds = DbIssueVariants.createdAfter(Now - 7.days).issueIds
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
		ArgumentSchema("since", "t", 7.days, help = "The duration or time since which issues should be scanned for"),
		ArgumentSchema("level", "lvl", Debug.level,
			help = "The lowest level of issues to include [1,6] where 1 represents debug information and 6 represents critical failures.")) { args =>
		// Parses the arguments
		val since = args("since").castTo(InstantType, DurationType) match {
			case Left(instantV) => instantV.getInstant
			case Right(durationV) => Now - durationV.getDuration
		}
		val minLevel = Severity.fromValue(args("level"))
		// Finds the issues
		cPool.tryWith { implicit c =>
			val occurrencesPerVariantId = DbIssueOccurrences.since(since).pull.groupBy { _.caseId }
			if (occurrencesPerVariantId.isEmpty)
				Vector()
			else {
				val variantsPerIssueId = DbIssueVariants(occurrencesPerVariantId.keySet).pull
					.map { v => v.withOccurrences(occurrencesPerVariantId(v.id).reverseSorted) }
					.groupBy { _.issueId }
				val issues = DbIssues(variantsPerIssueId.keySet).withSeverityAtLeast(minLevel).pull.map { issue =>
					issue.withOccurrences(variantsPerIssueId(issue.id).reverseSorted)
				}.reverseSorted
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
					issues.iterator.take(15).foreach { issue => println(s"\t- ${issue.id}: $issue${
						if (issue.variants.exists { _.created > since }) "NEW" else "" }") }
					if (issues.hasSize > 15)
						println("\t- ...")
				}
				
				// Writes instructions
				println("For more information concerning these issues, use 'see next' or 'see <issue id>'")
			
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
							issue.withOccurrences(DbIssueOccurrences.forVariants(issue.variants.map { _.id }).pull)
						}
					case Right(issue) => Some(issue)
				}
				issue match {
					case Some(issue) =>
						// Retrieves and prints available information about the issue
						val lastOccurrence = issue.latestOccurrence
						val numberOfOccurrences = DbIssueOccurrences.forVariants(issue.variantIds)
							.before(issue.earliestOccurrence match {
								case Some(o) => o.lastOccurrence
								case None => recent
							})
							.counts.sum + issue.numberOfOccurrences
						val averageOccurrenceInterval = (Now - issue.created) / numberOfOccurrences
						val affectedVersions = issue.variants.map { _.version }.minMaxOption
						
						println(s"${issue.id}: ${issue.severity} @ ${issue.context}")
						println(s"\t- First encountered ${timeDescription(issue.created)}")
						lastOccurrence.foreach { last =>
							println("s\t- Last occurrence:")
							describeOccurrence(last)
						}
						println(s"\t- $numberOfOccurrences occurrences in total")
						println(s"\t- Has occurred once every ${averageOccurrenceInterval.description}")
						issue.earliestOccurrence.foreach { first =>
							println(s"\t\t- Recently once every ${
								((Now - first.firstOccurrence) / issue.numberOfOccurrences).description}")
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
							println("For more information about each variant, use the 'next' command")
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
			variants.lift(nextIndex) -> (variants -> (nextIndex + 1))
		} match {
			// Case: Variant found
			case Some(variant) =>
				// Groups similar consecutive variant issues together (based on messages and details)
				val occurrenceIterator = variant.occurrences.reverseSorted.iterator
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
				
				println(s"Variant of issue ${variant.issueId}")
				println(s"\t- Version: ${variant.version}")
				variant.details.notEmpty.foreach { details =>
					println(s"\t- Variant details:")
					details.properties.foreach { detail =>
						println(s"\t\t- ${detail.name.capitalize}: ${detail.value}")
					}
				}
				println(s"\t- First appeared at ${timeDescription(variant.created)}")
				occurrenceIterator.nextOption().foreach { last =>
					println("\t- Latest occurrence:")
					describeOccurrence(last)
					if (variant.occurrences.hasSize > 1) {
						println(s"\t- Recently there have been ${ variant.numberOfOccurrences } occurrences")
						println(s"\t\t- Which averages one in every ${
							((Now - variant.earliestOccurrence.get.firstOccurrence) / variant.numberOfOccurrences).description}")
					}
					else
						println("\t\t- This is the only recent appearance of this variant")
				}
				queuedErrorIdPointer.value = variant.errorId
				if (variant.errorId.isDefined)
					println("For more information about the associated error, use the 'stack' command")
				queuedOccurrences = occurrenceIterator
				if (occurrenceIterator.hasNext)
					println("For information about previous issue occurrences, use the 'more' command")
				
			// Case: No more variants found
			case None => println("No more variants have been queued")
		}
	}
	private val moreOccurrencesCommand = Command.withoutArguments("more",
		help = "Prints information about another set of queued issue occurrences") {
		queuedOccurrences.nextOption() match {
			case Some(occurrence) =>
				println(s"\tOccurrence of issue variant ${occurrence.caseId}:")
				describeOccurrence(occurrence)
				if (queuedOccurrences.hasNext)
					println("For information about the next set of occurrences, use this same command")
			case None => println("No more occurrences have been queued")
		}
	}
	private val stackCommand = Command.withoutArguments("stack",
		help = "Prints the stack trace of the most recently encountered error") {
		queuedErrorIdPointer.value match {
			case Some(errorId) =>
				cPool.tryWith { implicit c =>
					// Reads and prints error data
					DbErrorRecord(errorId).topToBottomIterator.zipWithIndex.foreach { case (error, index) =>
						val prefix = if (index == 0) "" else "\tCaused by: "
						println(s"$prefix${error.exceptionType}")
						error.stackAccess.topToBottomIterator.groupBy { _.className }
							.foreach { case (className, stack) =>
								stack.iterator.groupBy { _.methodName }.toVector.oneOrMany match {
									case Left((methodName, lines)) =>
										println(s"\t$className.$methodName: [${lines.map { _.lineNumber }.mkString(", ")}]")
									case Right(methods) =>
										println(s"\t$className")
										methods.foreach { case (methodName, lines) =>
											println(s"\t\t$methodName: [${ lines.map { _.lineNumber }.mkString(", ") }]")
										}
								}
							}
					}
					
				}.logFailure
			case None => println("There is no error to describe")
		}
	}
	
	// Starts the console
	println("Welcome to the Scribe utility console!")
	println("You will find the available commands with the 'help' command.")
	Console.static(Vector(statusCommand, listCommand, seeCommand, nextCommand, moreOccurrencesCommand, stackCommand),
		"\nNext command:", "exit")
		.run()
	println("Bye!")
	
	
	// OTHER FUNCTIONS  -------------------------
	
	// Describes at indentation 2
	private def describeOccurrence(occurrence: IssueOccurrenceData) = {
		if (occurrence.count > 1)
			println(s"\t\t- ${occurrence.count} occurrences, which appeared between ${
				timeDescription(occurrence.firstOccurrence)} and ${timeDescription(occurrence.lastOccurrence)} (during ${
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

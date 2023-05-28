package utopia.scribe.api.app.console

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.mutable.DataType.{DurationType, InstantType, IntType, StringType}
import utopia.flow.operator.Identity
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.Now
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
import utopia.vault.database.ConnectionPool

import java.time.Instant
import scala.util.{Failure, Success}

/**
  * A command-line application that provides an interface for checking issue status
  * @author Mikko Hilpinen
  * @since 25.5.2023, v0.1
  */
object ScribeConsoleApp extends App
{
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
	
	// Specifies some computed properties
	
	private def recent = Now - 7.days
	
	// Sets up the commands for the console
	
	// Summarizes active and new issues
	private val statusCommand = Command.withoutArguments("status", "st", "Shows current issue status") {
		cPool.tryWith { implicit c =>
			// Counts active issues
			val countBySeverity = DbManyIssueInstances.since(recent).severities
				.groupMapReduce(Identity) { _ => 1 } { _ + _ }
			
			// Case: No active issues
			if (countBySeverity.isEmpty)
				println("No active issues within the last 7 days")
			// Case: Active issues => Lists number of active issues by severity and handles new issues
			else {
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
					println(s"${newIssueIds.size} of the issues have appeared just recently:")
					newIssueIds.foreach { case (issueId, variantCount) =>
						if (variantCount > 1)
							println(s"\t- $issueId - $variantCount new variants")
						else
							println(s"\t- $issueId")
					}
					queuedIssuesPointer.value = newIssueIds.map { case (id, _) => Left(id) } -> 0
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
							.counts.sum
						val averageOccurrenceInterval = (Now - issue.created) / numberOfOccurrences
						val affectedVersions = issue.variants.map { _.version }.minMaxOption
						
						println(s"${issue.id}: ${issue.severity} @ ${issue.context}")
						println(s"- First encountered at ${timeDescription(issue.created)}")
						lastOccurrence.foreach { last =>
							println(s"- Last encountered at ${timeDescription(last.lastOccurrence)}")
							last.errorMessages.reverseIterator.foreach { error =>
								println(s"\t- $error")
							}
						}
						println(s"- $numberOfOccurrences of occurrences in total")
						println(s"- Has occurred once every ${averageOccurrenceInterval.description}")
						issue.earliestOccurrence.foreach { first =>
							println(s"\t- Recently once every ${
								((Now - first.firstOccurrence) / issue.numberOfOccurrences).description}")
						}
						affectedVersions.foreach { versions =>
							if (versions.isSymmetric)
								println(s"- Affects version: ${ versions.first }")
							else
								println(s"- Affects versions: [${ versions.mkString(" - ") }]")
						}
						if (issue.variants.isEmpty)
							println("- Has not appeared recently")
						else {
							println(s"- Has ${issue.variants.size} different variants")
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
				println(s"Variant of issue ${variant.issueId}")
				println(s"- Version: ${variant.version}")
				variant.details.notEmpty.foreach { details => println(s"- Details: $details") }
				println(s"- First appeared at ${timeDescription(variant.created)}")
				variant.latestOccurrence.foreach { last =>
					println(s"- Last appeared at ${timeDescription(last.lastOccurrence)}")
					if (variant.occurrences.hasSize > 1) {
						println(s"- Recently there have been ${ variant.numberOfOccurrences } occurrences")
						println(s"\t- Which averages one in every ${
							((Now - variant.earliestOccurrence.get.firstOccurrence) / variant.numberOfOccurrences).description}")
					}
					else
						println("\t- Which is the only recent appearance of this variant")
				}
				variant.occurrences.reverseIterator.find { _.errorMessages.nonEmpty }.foreach { occurrence =>
					if (occurrence.errorMessages.hasSize > 1) {
						println(s"- Most recent \"story\":")
						occurrence.errorMessages.reverseIterator.foreach { msg =>
							println(s"\t- $msg")
						}
					}
					else
						println(s"- Message: ${occurrence.errorMessages.head}")
				}
				queuedErrorIdPointer.value = variant.errorId
				if (variant.errorId.isDefined)
					println("For more information about the associated error, use the 'stack' command")
				
			// Case: No more variants found
			case None => println("No more variants have been queued")
		}
	}
	private val stackCommand = Command.withoutArguments("stack",
		help = "Prints the stack trace of the most recently encountered error") {
		queuedErrorIdPointer.value match {
			case Some(errorId) =>
				cPool.tryWith { implicit c =>
					// Reads and prints error data
					DbErrorRecord(errorId).topToBottomIterator.zipWithIndex.foreach { case (error, index) =>
						val prefix = if (index == 0) "" else "\tCause by: "
						println(s"$prefix: ${error.exceptionType}")
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
	Console.static(Vector(statusCommand, listCommand, seeCommand, nextCommand, stackCommand),
		"Next command:", "exit")
		.run()
	println("Bye!")
	
	// OTHER FUNCTIONS  -------------------------
	
	private def timeDescription(time: Instant) = s"${time.toLocalDateTime} (${ (Now - time).description } ago)"
}

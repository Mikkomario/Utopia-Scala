package utopia.scribe.api.app.console

import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.mutable.DataType.{DurationType, InstantType}
import utopia.flow.operator.Identity
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.console.{ArgumentSchema, Command}
import utopia.flow.util.logging.{FileLogger, Logger, SysErrLogger}
import utopia.flow.view.mutable.Pointer
import utopia.scribe.api.database.access.many.logging.issue.{DbIssues, DbManyIssueInstances}
import utopia.scribe.api.database.access.many.logging.issue_occurrence.DbIssueOccurrences
import utopia.scribe.api.database.access.many.logging.issue_variant.DbIssueVariants
import utopia.scribe.api.util.ScribeContext
import utopia.scribe.core.model.combined.logging.IssueInstances
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.enumeration.Severity.Debug
import utopia.vault.database.ConnectionPool

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
	
	// Sets up the commands for the console
	
	// Summarizes active and new issues
	private val statusCommand = Command.withoutArguments("status", "st", "Shows current issue status") {
		cPool.tryWith { implicit c =>
			// Counts active issues
			val countBySeverity = DbManyIssueInstances.since(Now - 7.days).severities
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
}

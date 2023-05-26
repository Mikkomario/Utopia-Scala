package utopia.scribe.api.app.console

import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.mutable.DataType.{DurationType, InstantType, IntType, StringType}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.console.{ArgumentSchema, Command, CommandArgumentsSchema}
import utopia.flow.util.logging.{FileLogger, Logger, SysErrLogger}
import utopia.scribe.api.app.console.ScribeConsoleSettings.loaded
import utopia.scribe.api.database.access.many.logging.issue.DbIssues
import utopia.scribe.api.database.access.many.logging.issue_occurrence.DbIssueOccurrences
import utopia.scribe.api.database.access.many.logging.issue_variant.DbIssueVariants
import utopia.scribe.api.util.ScribeContext
import utopia.scribe.core.model.enumeration.Severity
import utopia.scribe.core.model.enumeration.Severity.Debug
import utopia.vault.database.ConnectionPool

/**
  * A command-line application that provides an interface for checking issue status
  * @author Mikko Hilpinen
  * @since 25.5.2023, v0.1
  */
object ScribeConsoleApp extends App
{
	// Initializes settings and context (may fail)
	
	ScribeConsoleSettings.initializeDbSettings().get
	
	private implicit val exc: ThreadPool = new ThreadPool("Scribe-Console", 3, 100, 10.seconds)
	private implicit lazy val logger: Logger = ScribeConsoleSettings.logDirectory match {
		case Some(dir) => new FileLogger(dir, 1.seconds, copyToSysErr = true)
		case None => SysErrLogger
	}
	private implicit val cPool: ConnectionPool = new ConnectionPool(30)
	
	ScribeContext.setup(exc, cPool, ScribeConsoleSettings.dbName, backupLogger = logger)
	
	// Sets up the commands for the console
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
		// Finds and lists the issues
		cPool.tryWith { implicit c =>
			val occurrencesPerVariantId = DbIssueOccurrences.since(since).pull.groupBy { _.caseId }
			if (occurrencesPerVariantId.isEmpty)
				println(s"There are no issues since ${since}")
			else {
				val variants = DbIssueVariants(occurrencesPerVariantId.keySet).pull
					.map { v => v.withOccurrences(occurrencesPerVariantId(v.id)) }
					.groupBy { _.issueId }
				// TODO: Continue
			}
		}.logFailure
	}
}

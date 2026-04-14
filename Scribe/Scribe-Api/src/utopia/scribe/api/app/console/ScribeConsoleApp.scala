package utopia.scribe.api.app.console

import utopia.flow.async.context.ThreadPool
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.file.FileUtils
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.StringExtensions._
import utopia.flow.util.Version
import utopia.flow.util.console.Console
import utopia.flow.util.logging.{FileLogger, Logger, SysErrLogger}
import utopia.scribe.api.util.ScribeContext
import utopia.vault.database.ConnectionPool
import utopia.vault.database.columnlength.ColumnLengthRules

/**
  * A command-line application that provides an interface for checking issue status
  * @author Mikko Hilpinen
  * @since 25.5.2023, v0.1
  */
object ScribeConsoleApp extends App
{
	// SETUP   -------------------
	
	private implicit val version: Version = Version(1, 2)
	
	// Initializes settings and context (may fail)
	ScribeConsoleSettings.logDirectory = "log"
	ScribeConsoleSettings.setupDb(allowUserInteraction = true).get
	
	private implicit val jsonParser: JsonParser = ScribeConsoleSettings.jsonParser
	private implicit val exc: ThreadPool = ScribeConsoleSettings.threadPool
	private implicit val log: Logger = ScribeConsoleSettings.logDirectory match {
		case Some(dir) => new FileLogger(dir, 1.seconds, copyToSysErr = true)
		case None => SysErrLogger
	}
	private implicit val cPool: ConnectionPool = ScribeConsoleSettings.cPool
	
	// Loads column length rules, if possible
	private val lengthRuleKeys = Vector("scribe", "length", "rule", "json")
	FileUtils.workingDirectory.toTree.topDownNodesIterator
		.findMap { _.nav.iterateChildren { _.find { _.fileName.containsInOrder(lengthRuleKeys) } }.getOrElse(None) }
		.foreach { ColumnLengthRules.loadFrom(_, ScribeContext.databaseName) }
	
	private val reviewCommands = new LogReviewCommands()
	private val manageCommands = new ManageIssueCommands(reviewCommands.openIssueIdPointer)
	private val commandsP = reviewCommands.pointer.mergeWith(manageCommands.pointer) { (review, manage) =>
		Map("review" -> review, "manage" -> manage)
	}
	
	
	// APP CODE --------------------------------
	
	// Starts the console
	println("Welcome to the Scribe utility console!")
	println("You will find the available commands with the 'help' command.")
	Console.namespaced(commandsP, s"\nNext command", closeCommandName = "exit", listAvailableCommands = true).run()
	println("Bye!")
}

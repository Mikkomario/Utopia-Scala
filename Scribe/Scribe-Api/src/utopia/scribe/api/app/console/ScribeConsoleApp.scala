package utopia.scribe.api.app.console

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.context.ThreadPool
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.Version
import utopia.flow.util.console.Console
import utopia.flow.util.logging.{FileLogger, Logger, SysErrLogger}
import utopia.flow.view.immutable.caching.Lazy
import utopia.scribe.api.util.ScribeContext
import utopia.vault.database.{ConnectionPool, Tables}

/**
  * A command-line application that provides an interface for checking issue status
  * @author Mikko Hilpinen
  * @since 25.5.2023, v0.1
  */
object ScribeConsoleApp extends App
{
	// SETUP   -------------------
	
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
	
	// TODO: Set the correct APP version
	private val appVersion = Lazy.initialized(Version(1))
	ScribeContext.setup(exc, cPool, new Tables(cPool), ScribeConsoleSettings.dbName, backupLogger = log)
	
	private val reviewCommands = new LogReviewCommands(appVersion)
	
	
	// APP CODE --------------------------------
	
	// Starts the console
	println("Welcome to the Scribe utility console!")
	println("You will find the available commands with the 'help' command.")
	Console(reviewCommands.pointer,
		s"\nNext command (${ reviewCommands.currently.iterator.map { _.name }.mkString(" | ") } | exit):",
		closeCommandName = "exit")
		.run()
	println("Bye!")
}

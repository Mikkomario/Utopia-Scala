package utopia.scribe.api.test

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.context.{Scheduler, ThreadPool}
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.Version
import utopia.flow.util.logging.SysErrLogger
import utopia.flow.util.console.ConsoleExtensions._
import utopia.scribe.api.controller.logging.Scribe
import utopia.scribe.api.util.ScribeContext
import utopia.vault.database.{Connection, ConnectionPool, Tables}
import utopia.vault.database.columnlength.ColumnLengthRules
import utopia.vault.error.ErrorHandler.Rethrow
import utopia.vault.error.HandleError

import scala.concurrent.ExecutionContext
import scala.io.StdIn

/**
  * Used for setting up a testing environment for Scribe-testing
  * @author Mikko Hilpinen
  * @since 3.8.2023, v1.0
  */
object ScribeTestContext
{
	implicit val exc: ExecutionContext = new ThreadPool("Scribe-Test")(SysErrLogger)
	implicit val scheduler: Scheduler = Scheduler.newInstance(exc, SysErrLogger)
	implicit val cPool: ConnectionPool = new ConnectionPool(25, 5, 5.seconds)
	implicit val jsonParser: JsonParser = JsonBunny
	
	private val dbUser = StdIn.read("Please specify the DB user (default = root)").stringOr("root")
	private val dbPassword = StdIn.read("Please specify the DB password (default = no password)").getString
	val databaseName = StdIn.read("Please specify the name of the targeted database (default = utopia_scribe_db)")
		.stringOr("utopia_scribe_db")
	
	Connection.modifySettings { _.copy(user = dbUser, password = dbPassword, defaultDBName = Some(databaseName)) }
	
	ScribeContext.setup(exc, scheduler, cPool, new Tables(cPool), databaseName, version = Version(1, 2, 4))
	HandleError.default = Rethrow
	ColumnLengthRules.loadFrom("Scribe/Scribe-Core/data/length-rules/scribe-length-rules-v0.1.json",
		"utopia_scribe_db")
	implicit val scribe: Scribe = Scribe("Test")
}

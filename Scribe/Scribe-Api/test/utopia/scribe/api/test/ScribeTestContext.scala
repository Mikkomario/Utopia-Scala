package utopia.scribe.api.test

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.context.ThreadPool
import utopia.flow.parse.json.JsonParser
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.SysErrLogger
import utopia.scribe.api.controller.logging.Scribe
import utopia.scribe.api.util.ScribeContext
import utopia.vault.database.{ConnectionPool, Tables}
import utopia.vault.database.columnlength.ColumnLengthRules
import utopia.vault.util.{ErrorHandling, ErrorHandlingPrinciple}

import scala.concurrent.ExecutionContext

/**
  * Used for setting up a testing environment for Scribe-testing
  * @author Mikko Hilpinen
  * @since 3.8.2023, v1.0
  */
object ScribeTestContext
{
	implicit val exc: ExecutionContext = new ThreadPool("Scribe-Test")(SysErrLogger)
	implicit val cPool: ConnectionPool = new ConnectionPool(25, 5, 5.seconds)
	implicit val jsonParser: JsonParser = JsonBunny
	
	ScribeContext.setup(exc, cPool, new Tables(cPool))
	ErrorHandling.defaultPrinciple = ErrorHandlingPrinciple.Throw
	ColumnLengthRules.loadFrom("Scribe/Scribe-Core/data/length-rules/scribe-length-rules-v0.1.json",
		"utopia_scribe_db")
	implicit val scribe: Scribe = Scribe("Test")
}

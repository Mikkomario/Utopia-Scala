package utopia.scribe.api.test

import utopia.bunnymunch.jawn.JsonBunny
import utopia.flow.async.context.ThreadPool
import utopia.flow.async.process.Wait
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.parse.file.FileExtensions._
import utopia.flow.parse.json.JsonParser
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.SysErrLogger
import utopia.scribe.api.controller.logging.Scribe
import utopia.scribe.api.database.ScribeAccessExtensions._
import utopia.scribe.api.database.access.many.logging.issue.DbIssues
import utopia.scribe.api.database.access.single.logging.error_record.DbErrorRecord
import utopia.scribe.api.util.ScribeContext
import utopia.scribe.core.model.enumeration.Severity.Debug
import utopia.scribe.core.util.logging.TryExtensions._
import utopia.vault.database.columnlength.ColumnLengthRules
import utopia.vault.database.{ConnectionPool, Tables}
import utopia.vault.util.{ErrorHandling, ErrorHandlingPrinciple}

import scala.concurrent.ExecutionContext
import scala.util.Try

/**
  * Tests database logging and log-entry reading
  * @author Mikko Hilpinen
  * @since 11.7.2023, v1.0
  */
object DbLoggingTest extends App
{
	implicit val exc: ExecutionContext = new ThreadPool("Scribe-Test")(SysErrLogger)
	implicit val cPool: ConnectionPool = new ConnectionPool(25, 5, 5.seconds)
	implicit val jsonParser: JsonParser = JsonBunny
	
	ScribeContext.setup(exc, cPool, new Tables(cPool))
	ErrorHandling.defaultPrinciple = ErrorHandlingPrinciple.Throw
	ColumnLengthRules.loadFrom("Scribe/Scribe-Core/data/length-rules/scribe-length-rules-v0.1.json",
		"utopia_scribe_db")
	implicit val scribe: Scribe = Scribe("Test")
	
	def testFunction() = {
		throw new IllegalStateException("Test Error")
	}
	// Connection.modifySettings { _.copy(debugPrintsEnabled = true) }
	
	Try { testFunction() }
		.logWith("Test function failed (expected)", subContext = "testFunction", severity = Debug)
	
	// Waits in order to make sure the error is recorded asynchronously
	Wait(2.5.seconds)
	
	cPool { implicit c =>
		val issues = DbIssues.instances.since(Now - 5.seconds, includePartialRanges = true).pull
		println(s"Read ${ issues.size } issues from the DB")
		issues.foreach { issue =>
			println(s"${issue.id} ${issue.severity} ${issue.context}")
			issue.variants.foreach { variant =>
				println(s"\t- Variant #${variant.id} ${variant.version}")
				variant.latestOccurrence.foreach { occ =>
					println(s"\t\t- ${occ.lastOccurrence}: ${occ.errorMessages}")
				}
				variant.errorId.foreach { errorId =>
					println(s"Describing error $errorId")
					DbErrorRecord(errorId).topToBottomIterator.foreach { error =>
						println(error.data.exceptionType)
						error.stackAccess.topToBottomIterator.groupBy { _.className }
							.foreach { case (className, stack) =>
								stack.iterator.groupBy { _.methodName }.toVector.oneOrMany match {
									case Left((methodName, lines)) =>
										println(s"\t$className.$methodName: [${ lines.map { _.lineNumber }.mkString(", ") }]")
									case Right(methods) =>
										println(s"\t$className")
										methods.foreach { case (methodName, lines) =>
											println(s"\t\t$methodName: [${ lines.map { _.lineNumber }.mkString(", ") }]")
										}
								}
							}
					}
				}
			}
		}
		println("Issue documented")
	}
}

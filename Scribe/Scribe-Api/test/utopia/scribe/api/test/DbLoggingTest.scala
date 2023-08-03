package utopia.scribe.api.test

import utopia.flow.async.process.Wait
import utopia.flow.collection.CollectionExtensions._
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Model
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.scribe.api.database.ScribeAccessExtensions._
import utopia.scribe.api.database.access.many.logging.issue.DbIssues
import utopia.scribe.api.database.access.single.logging.error_record.DbErrorRecord
import utopia.scribe.core.model.enumeration.Severity.Debug
import utopia.scribe.core.util.logging.TryExtensions._

import scala.util.Try

/**
  * Tests database logging and log-entry reading
  * @author Mikko Hilpinen
  * @since 11.7.2023, v1.0
  */
object DbLoggingTest extends App
{
	import ScribeTestContext._
	
	def testFunction() = {
		throw new IllegalStateException("Test Error")
	}
	// Connection.modifySettings { _.copy(debugPrintsEnabled = true) }
	
	Try { testFunction() }
		.logWith("Test function failed (expected)", subContext = "testFunction", severity = Debug,
			variantDetails = Model.from("firstValue" -> 1, "secondValue" -> "test"))
	
	// Waits in order to make sure the error is recorded asynchronously
	Wait(1.0.seconds)
	
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
						error.stackAccess.topToBottomIterator.groupBy { _.fileAndClassName }
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

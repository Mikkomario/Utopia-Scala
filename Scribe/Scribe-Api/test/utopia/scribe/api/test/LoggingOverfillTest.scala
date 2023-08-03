package utopia.scribe.api.test

import utopia.flow.async.process.Wait
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.time.TimeExtensions._
import utopia.scribe.api.controller.logging.Scribe
import utopia.scribe.api.database.access.single.logging.issue.DbIssue
import utopia.scribe.core.model.enumeration.Severity.Debug

/**
  * Tests logging maximum capacity
  * @author Mikko Hilpinen
  * @since 3.8.2023, v1.0
  */
object LoggingOverfillTest extends App
{
	import ScribeTestContext._
	
	val _scribe = scribe.in("LoggingOverfillTest").debug
	val noOverfill = _scribe.variant("shouldOverfill", false)
	val overfill = _scribe.variant("shouldOverfill", true)
	
	var overfillEventReceived = false
	
	// Connection.modifySettings { _.copy(debugPrintsEnabled = true) }
	
	println("Test starting. Estimated duration: 4 seconds")
	
	// Sets up maximum logging
	Scribe.setupLoggingLimit(3, 1.0.seconds)
	Scribe.addLoggingLimitReachedListener { _ =>
		assert(!overfillEventReceived)
		overfillEventReceived = true
	}
	
	// Logs "normally"
	noOverfill("Testing 1.1")
	noOverfill("Testing 1.2")
	
	Wait(2.0.seconds)
	
	// Proceeds to over-log
	noOverfill("Testing 2.1")
	noOverfill("Testing 2.2")
	noOverfill("Testing 2.3")
	assert(!overfillEventReceived)
	overfill("Testing 2.4")
	assert(overfillEventReceived)
	overfill("Testing 2.5")
	
	Wait(2.0.seconds)
	
	// Attempts to log after overfill
	overfill("Testing 3.1")
	
	// Tests whether the output is correct
	cPool { implicit c =>
		val issue = DbIssue.specific(_scribe.context, Debug).withInstances.pull.get
		lazy val debugStr = issue.toString
		assert(issue.variants.size == 1, debugStr)
		
		val variant = issue.variants.head
		assert(variant.details("shouldOverfill").boolean.contains(false), debugStr)
		assert(variant.occurrences.size == 5, debugStr)
		
		// Clears the state for the next test
		DbIssue(issue.id).delete()
	}
	
	println("Done!")
}

package utopia.flow.util

import utopia.flow.time.Now

import java.time.Instant
import utopia.flow.time.TimeExtensions._

/**
  * Used for tracking actions and logging tasks with durations
  * @author Mikko Hilpinen
  * @since 12.8.2020, v1.2
  */
class TimeLogger
{
	// ATTRIBUTES	-------------------------
	
	private var startTime = Instant.now()
	
	
	// OTHER	-----------------------------
	
	/**
	  * Prints a check point, resetting time counter as well
	  * @param description Printed description
	  */
	def checkPoint(description: String) =
	{
		println(description + s" (${(Instant.now() - startTime).description})")
		startTime = Now.toInstant
	}
}

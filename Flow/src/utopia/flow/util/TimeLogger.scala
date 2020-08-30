package utopia.flow.util

import java.time.Instant

import TimeExtensions._

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
		startTime = Instant.now()
	}
}

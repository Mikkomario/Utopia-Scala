package utopia.flow.util.logging

import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._

import scala.collection.immutable.VectorBuilder

/**
  * Used for tracking actions and logging tasks with durations
  * @author Mikko Hilpinen
  * @since 12.8.2020, v1.2
  */
class TimeLogger
{
	// ATTRIBUTES	-------------------------
	
	private var startTime = Now.toInstant
	private val linesBuilder = new VectorBuilder[String]()
	
	
	// OTHER	-----------------------------
	
	/**
	  * Prints a check point, resetting time counter as well
	  * @param description Printed description
	  */
	def checkPoint(description: String) =
	{
		val time = Now.toInstant
		linesBuilder += (description + s" (${(time - startTime).description})")
		startTime = time
	}
	
	def print() = {
		linesBuilder.result().foreach { println(_) }
		linesBuilder.clear()
	}
}

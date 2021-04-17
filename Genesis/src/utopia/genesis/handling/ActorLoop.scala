package utopia.genesis.handling

import java.time.Instant

import utopia.flow.async.Loop
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.WaitTarget.Until
import utopia.genesis.util.Fps

class ActorLoop(private val handler: ActorHandler, val apsRange: Range = 15 to 60) extends Loop
{
	// ATTRIBUTES	-------------------
	
	/**
	  * Minimum actions per second
	  */
	val minAPS = Fps(apsRange.start)
	
	/**
	  * Maximum actions per second
	  */
	val maxAPS = Fps(apsRange.end)
	
	/**
	  * @return The minimum interval between act calls
	  */
	def minInterval = maxAPS.interval
	
	/**
	  * @return The maximum interval passed to act calls (actual interval may be longer)
	  */
	def maxInterval = minAPS.interval
	
	private var lastActStarted = Instant.now()
	
	
	// IMPLEMENTED	-------------------
	
	/**
	  * Calls act(...) of all associated Actors
	  */
	override def runOnce() =
	{
		val actStarted = Instant.now()
		// MaxAPS may affect calculations so that the real time lapse is not used
		// This will result in program "slowdown"
		val sinceLastAct = (actStarted - lastActStarted) min maxInterval
		lastActStarted = actStarted
		
		handler.act(sinceLastAct)
	}
	
	/**
	  * The time between the end of the current run and the start of the next one
	  */
	override def nextWaitTarget = Until(lastActStarted + minInterval)
}

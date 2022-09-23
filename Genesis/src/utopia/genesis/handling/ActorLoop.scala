package utopia.genesis.handling

import utopia.flow.async.process.LoopingProcess

import java.time.Instant
import utopia.flow.time.TimeExtensions._
import utopia.flow.time.WaitTarget.Until
import utopia.flow.util.logging.Logger
import utopia.genesis.util.Fps

import scala.concurrent.ExecutionContext

class ActorLoop(handler: ActorHandler, val apsRange: Range = 15 to 60)(implicit exc: ExecutionContext, logger: Logger)
	extends LoopingProcess
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
	
	private var lastActStarted = Instant.now()
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return The minimum interval between act calls
	  */
	def minInterval = maxAPS.interval
	/**
	  * @return The maximum interval passed to act calls (actual interval may be longer)
	  */
	def maxInterval = minAPS.interval
	
	
	// IMPLEMENTED	-------------------
	
	override protected def isRestartable = true
	
	override protected def iteration() =
	{
		val actStarted = Instant.now()
		// MaxAPS may affect calculations so that the real time lapse is not used
		// This will result in program "slowdown"
		val sinceLastAct = (actStarted - lastActStarted) min maxInterval
		lastActStarted = actStarted
		
		handler.act(sinceLastAct)
		
		Some(Until(lastActStarted + minInterval))
	}
}

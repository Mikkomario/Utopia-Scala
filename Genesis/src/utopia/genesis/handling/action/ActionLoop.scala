package utopia.genesis.handling.action

import utopia.flow.async.process.LoopingProcess
import utopia.flow.async.process.WaitTarget.Until
import utopia.flow.collection.immutable.range.{HasInclusiveEnds, Span}
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.logging.Logger
import utopia.genesis.util.Fps

import java.time.Instant
import scala.concurrent.ExecutionContext

/**
  * A loop that continuously generates action events
  * @param actor The actor that will receive the generated events.
  *              If multiple actors should be informed, use an [[ActorHandler]]
  * @param apsRange The valid range of actions-per-second.
  *                 Will attempt to fire an action event at the maximum APS.
  *
  *                 If the actual APS falls below the minimum value, starts **simulating** larger APS
  *                 values. In the program this should appear as a slow-down effect.
  *                 This is a safeguard against situations where acting based on a real, very low APS
  *                 would result in undesired results (e.g. objects passing each other in a physics simulation).
  *
  *                 The default APS range is from 15 actions-per-second to 60 actions-per-second.
  *                 I.e. Action events are fired 60 times a second by default,
  *                 and the program will start to "lag" if 15+ actions-per-second cannot be maintained.
  *
  * @param exc Implicit execution context
  * @param logger Logger that records exceptions caught during the scheduled actions
  */
class ActionLoop(actor: Actor, val apsRange: HasInclusiveEnds[Fps] = Span(Fps(15), Fps(60)))
                (implicit exc: ExecutionContext, logger: Logger)
	extends LoopingProcess
{
	// ATTRIBUTES	-------------------
	
	private var lastActStarted = Instant.now()
	
	
	// COMPUTED -----------------------
	
	/**
	  * Minimum actions per second
	  */
	def minAPS = apsRange.start
	/**
	  * Maximum actions per second
	  */
	def maxAPS = apsRange.end
	
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
	
	override protected def iteration() = {
		val actStarted = Instant.now()
		// MaxAPS may affect calculations so that the real time lapse is not used
		// This will result in program "slowdown"
		val sinceLastAct = (actStarted - lastActStarted) min maxInterval
		lastActStarted = actStarted
		
		actor.act(sinceLastAct)
		
		Some(Until(actStarted + minInterval))
	}
}

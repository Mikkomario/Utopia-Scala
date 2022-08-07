package utopia.paradigm.motion.motion2d

import utopia.paradigm.motion.template.VelocityTracker
import utopia.paradigm.shape.shape2d.Vector2D

import java.time.Instant
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Used for tracking two dimensional movement over time
  * @author Mikko Hilpinen
  * @since Genesis 22.7.2020, v2.3
  */
class VelocityTracker2D(maxHistoryDuration: Duration, minCacheInterval: Duration = Duration.Zero)
	extends VelocityTracker[Vector2D, Velocity2D, Acceleration2D, MovementStatus2D, MovementHistory2D](
		maxHistoryDuration, minCacheInterval)
{
	override protected def calculateVelocity(distance: Vector2D, duration: FiniteDuration) =
		distance.traversedIn(duration)
	
	override protected def calculateAcceleration(velocityChange: Velocity2D, duration: FiniteDuration) =
		velocityChange.acceleratedIn(duration)
	
	override protected def combineHistory(positionHistory: Vector[(Vector2D, Instant)],
										  velocityHistory: Vector[(Velocity2D, Instant)],
										  accelerationHistory: Vector[(Acceleration2D, Instant)]) =
		MovementHistory2D(positionHistory, velocityHistory, accelerationHistory)
	
	override protected def zeroPosition = Vector2D.zero
	
	override protected def zeroVelocity = Velocity2D.zero
	
	override protected def zeroAcceleration = Acceleration2D.zero
	
	override protected def combine(position: Vector2D, velocity: Velocity2D, acceleration: Acceleration2D) =
		MovementStatus2D(position, velocity, acceleration)
}

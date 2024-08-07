package utopia.paradigm.motion.motion3d

import utopia.paradigm.motion.template.VelocityTracker
import utopia.paradigm.shape.shape3d.Vector3D

import java.time.Instant
import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Used for tracking three dimensional position, velocity and acceleration
  * @author Mikko Hilpinen
  * @since Genesis 22.7.2020, v2.3
  */
class VelocityTracker3D(maxHistoryDuration: Duration, minCacheInterval: Duration = Duration.Zero)
	extends VelocityTracker[Vector3D, Velocity3D, Acceleration3D, MovementStatus3D, MovementHistory3D](
		maxHistoryDuration, minCacheInterval)
{
	override protected def zeroPosition = Vector3D.zero
	override protected def zeroVelocity = Velocity3D.zero
	override protected def zeroAcceleration = Acceleration3D.zero
	
	override protected def calculateVelocity(distance: Vector3D, duration: FiniteDuration) =
		distance.traversedIn(duration)
	
	override protected def calculateAcceleration(velocityChange: Velocity3D, duration: FiniteDuration) =
		velocityChange.acceleratedIn(duration)
	
	override protected def combineHistory(positionHistory: Seq[(Vector3D, Instant)],
										  velocityHistory: Seq[(Velocity3D, Instant)],
										  accelerationHistory: Seq[(Acceleration3D, Instant)]) =
		MovementHistory3D(positionHistory, velocityHistory, accelerationHistory)
	
	override protected def combine(position: Vector3D, velocity: Velocity3D, acceleration: Acceleration3D) =
		MovementStatus3D(position, velocity, acceleration)
}

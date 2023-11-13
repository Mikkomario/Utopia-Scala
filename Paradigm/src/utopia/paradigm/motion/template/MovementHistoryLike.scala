package utopia.paradigm.motion.template

import utopia.flow.collection.CollectionExtensions._
import utopia.flow.collection.immutable.Pair
import utopia.flow.operator.combine.{Combinable, LinearScalable}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.shape.template.vector.DoubleVectorLike

import java.time.Instant
import scala.concurrent.duration.FiniteDuration

/**
  * A common trait for items that contain an object's movement status over a time period
  * @author Mikko Hilpinen
  * @since Genesis 22.7.2020, v2.3
  */
trait MovementHistoryLike[X <: DoubleVectorLike[X], V <: VelocityLike[X, V], A <: AccelerationLike[X, V, A],
	+S <: MovementStatusLike[X, V, A, _]]
{
	// ABSTRACT	-----------------------------
	
	/**
	  * @return Recorded positions over a time period
	  */
	def positionHistory: Vector[(X, Instant)]
	
	/**
	  * @return Recorded velocities over a time period
	  */
	def velocityHistory: Vector[(V, Instant)]
	
	/**
	  * @return Recorded accelerations over a time period
	  */
	def accelerationHistory: Vector[(A, Instant)]
	
	/**
	  * @return A (0,0) position
	  */
	protected def zeroPosition: X
	
	/**
	  * @return A zero vector velocity
	  */
	protected def zeroVelocity: V
	
	/**
	  * @return A vero vector acceleration
	  */
	protected def zeroAcceleration: A
	
	/**
	  * @param position     A position
	  * @param velocity     A velocity
	  * @param acceleration An acceleration
	  * @return Combined status with these values
	  */
	protected def combine(position: X, velocity: V, acceleration: A): S
	
	
	// COMPUTED	----------------------------
	
	/**
	  * @return Latest recorded position
	  */
	def latestPosition = positionHistory.lastOption.map { _._1 }.getOrElse(zeroPosition)
	
	/**
	  * @return Latest recorded velocity
	  */
	def latestVelocity = velocityHistory.lastOption.map { _._1 }.getOrElse(zeroVelocity)
	
	/**
	  * @return Latest recorded acceleration
	  */
	def latestAcceleration = accelerationHistory.lastOption.map { _._1 }.getOrElse(zeroAcceleration)
	
	/**
	  * @return Latest recorded status
	  */
	def latestStatus = combine(latestPosition, latestVelocity, latestAcceleration)
	
	/**
	  * @return Current status, based on latest known status and its projection
	  */
	def projectedStatus = futureStatusAt(Now)
	
	/**
	  * @return Average position over recorded history
	  */
	def averagePosition = average(positionHistory, zeroPosition)
	
	/**
	  * @return Average velocity over recorded history
	  */
	def averageVelocity = average(velocityHistory, zeroVelocity)
	
	/**
	  * @return Average acceleration over recorded history
	  */
	def averageAcceleration = average(accelerationHistory, zeroAcceleration)
	
	/**
	  * @return Whether the tracked movement has stopped (velocity-wise)
	  */
	def isNotMoving = velocityHistory.lastOption.forall { _._1.isZero }
	
	/**
	  * @return Whether the tracked movement is still active (has velocity)
	  */
	def isMoving = !isNotMoving
	
	/**
	  * @return Whether the tracked movement has stopped accelerating or decelerating
	  */
	def isNotAccelerating = accelerationHistory.lastOption.forall { _._1.isZero }
	
	/**
	  * @return Whether the tracked movement is accelerating or decelerating
	  */
	def isAccelerating = !isNotAccelerating
	
	
	// OTHER	----------------------------
	
	/**
	  * @param time Target time point (should not be in the past) (call by name)
	  * @return Projected status at specified time point based on the latest known state
	  */
	def futureStatusAt(time: => Instant) =
	{
		positionHistory.lastOption match {
			case Some((lastPosition, lastTime)) =>
				// Checks velocity and acceleration that have affected the position since
				val velocity = averageVelocitySince(lastTime)
				val acceleration = averageAccelerationSince(lastTime)
				// Uses function: X = X0 + V0t + 1/2At^2 where X0 = last position, V0 is the velocity at the time
				// and A is the average acceleration
				val duration = time - lastTime
				val (travelVector, projectedVelocity) = velocity(duration, acceleration)
				combine(lastPosition + travelVector, projectedVelocity, acceleration)
			case None => latestStatus
		}
	}
	
	/**
	  * @param duration A time duration
	  * @return Projected movement status after specified duration has passed
	  */
	def futureStatusAfter(duration: => FiniteDuration) = futureStatusAt(Now + duration)
	
	/**
	  * @param threshold Time threshold
	  * @return Average position from history after specified time threshold
	  */
	def averagePositionSince(threshold: Instant) = averageSince(positionHistory, threshold, latestPosition)
	
	/**
	  * @param threshold Time threshold
	  * @return Average velocity from history after specified time threshold
	  */
	def averageVelocitySince(threshold: Instant) = averageSince(velocityHistory, threshold, latestVelocity)
	
	/**
	  * @param threshold Time threshold
	  * @return Average acceleration from history after specified time threshold
	  */
	def averageAccelerationSince(threshold: Instant) = averageSince(accelerationHistory, threshold, latestAcceleration)
	
	private def averageSince[Z <: Combinable[Z, Z] with LinearScalable[Z]](items: Vector[(Z, Instant)], threshold: Instant,
	                                                                       latest: => Z) =
	{
		val targetGroup = items.reverseIterator.takeWhile { _._2 >= threshold }.toVector
		average(targetGroup, latest)
	}
	
	private def average[Z <: Combinable[Z, Z] with LinearScalable[Z]](items: Vector[(Z, Instant)], zero: => Z) =
	{
		val targetGroupSize = items.size
		if (targetGroupSize == 0)
			zero
		else if (targetGroupSize == 1)
			items.head._1
		else {
			// Calculates weighted average based on duration of each segment
			val amountsWithDurations = (items.head._1 -> (Instant.now() - items.head._2)) +:
				items.paired.map { case Pair((_, latterTime), (previousItem, previousTime)) =>
					previousItem -> (latterTime - previousTime)
				}
			val totalWeightedAmount = amountsWithDurations.map { case (amount, duration) =>
				amount * duration.toNanos.toDouble
			}.reduce { _ + _ }
			val totalNanos = amountsWithDurations.map { _._2.toNanos }.sum
			
			totalWeightedAmount / totalNanos.toDouble
		}
	}
}

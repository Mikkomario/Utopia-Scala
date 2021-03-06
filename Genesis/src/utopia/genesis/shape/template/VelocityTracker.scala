package utopia.genesis.shape.template

import java.time.Instant
import utopia.flow.event.{ChangeDependency, ChangeListener, Changing}
import utopia.flow.time.Now
import utopia.flow.time.TimeExtensions._
import utopia.genesis.shape.shape2D.Vector2DLike

import scala.concurrent.duration.{Duration, FiniteDuration}

/**
  * Common trait for velocity tracker implementations, which are used for deducting velocity and acceleration from
  * position data
  * @author Mikko Hilpinen
  * @since 22.7.2020, v2.3
  */
abstract class VelocityTracker[X <: Vector2DLike[X], V <: VelocityLike[X, V], A <: AccelerationLike[X, V, A],
	+S <: MovementStatusLike[X, V, A, _], H]
(maxHistoryDuration: Duration, minCacheInterval: Duration = Duration.Zero)
	extends MovementHistoryLike[X, V, A, S] with Changing[H]
{
	// ATTRIBUTES	-----------------------
	
	override var listeners = Vector[ChangeListener[H]]()
	override var dependencies = Vector[ChangeDependency[H]]()
	
	private var _positionHistory = Vector[(X, Instant)]()
	private var _velocityHistory = Vector[(V, Instant)]()
	private var _accelerationHistory = Vector[(A, Instant)]()
	
	private var cachedValue: Option[H] = None
	
	
	// ABSTRACT	---------------------------
	
	protected def calculateVelocity(distance: X, duration: FiniteDuration): V
	
	protected def calculateAcceleration(velocityChange: V, duration: FiniteDuration): A
	
	protected def combineHistory(positionHistory: Vector[(X, Instant)], velocityHistory: Vector[(V, Instant)],
								 accelerationHistory: Vector[(A, Instant)]): H
	
	
	// COMPUTED	----------------------------
	
	/**
	  * @return A snapshot of this tracker's current state
	  */
	def currentState = value
	
	
	// IMPLEMENTED	------------------------
	
	override def isChanging = true
	
	override def value = cachedValue.getOrElse(combineHistory(_positionHistory, _velocityHistory, _accelerationHistory))
	
	override def positionHistory = _positionHistory
	
	override def velocityHistory = _velocityHistory
	
	override def accelerationHistory = _accelerationHistory
	
	
	// OTHER	---------------------------
	
	/**
	  * Records a position at a specific time point
	  * @param newPosition New recorded position
	  * @param eventTime Timestamp of the new position (default = current time)
	  */
	def recordPosition(newPosition: X, eventTime: Instant = Now) =
	{
		// May ignore some updates if they are too frequent
		if (minCacheInterval <= Duration.Zero ||
			_positionHistory.lastOption.forall { case (_, time) =>
				minCacheInterval.finite.exists { time <= eventTime - _ } })
		{
			val previousStatus = value
			
			// Updates position history
			val threshold = maxHistoryDuration.finite.map { eventTime - _ }
			val oldPositionHistory = _positionHistory
			threshold match
			{
				case Some(t) =>
					_positionHistory = _positionHistory.dropWhile { _._2 < t } :+ (newPosition, eventTime)
				case None => _positionHistory :+= newPosition -> eventTime
			}
			
			// Updates velocity history
			val newVelocity =
			{
				if (oldPositionHistory.nonEmpty)
				{
					val (lastPosition, lastEventTime) = oldPositionHistory.last
					calculateVelocity(newPosition - lastPosition, eventTime - lastEventTime)
				}
				else
					zeroVelocity
			}
			val oldVelocityHistory = _velocityHistory
			threshold match
			{
				case Some(t) =>
					_velocityHistory = _velocityHistory.dropWhile { _._2 < t } :+ (newVelocity, eventTime)
				case None => _velocityHistory :+= newVelocity -> eventTime
			}
			
			// Updates acceleration history
			val newAcceleration =
			{
				if (oldVelocityHistory.nonEmpty)
				{
					val (lastVelocity, lastEventTime) = oldVelocityHistory.last
					calculateAcceleration(newVelocity - lastVelocity, eventTime - lastEventTime)
				}
				else
					zeroAcceleration
			}
			threshold match
			{
				case Some(t) =>
					_accelerationHistory = _accelerationHistory.dropWhile { _._2 < t } :+ (newAcceleration, eventTime)
				case None => _accelerationHistory :+= newAcceleration -> eventTime
			}
			
			// Fires a change event and updates cached status
			cachedValue = Some(combineHistory(_positionHistory, _velocityHistory, _accelerationHistory))
			fireChangeEvent(previousStatus)
		}
	}
}

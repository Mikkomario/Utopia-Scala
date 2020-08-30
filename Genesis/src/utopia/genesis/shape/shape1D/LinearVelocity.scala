package utopia.genesis.shape.shape1D

import utopia.flow.util.RichComparable
import utopia.flow.util.TimeExtensions._
import utopia.genesis.shape.shape2D.{Vector2D, Velocity2D}
import utopia.genesis.shape.shape3D.{Vector3D, Velocity3D}
import utopia.genesis.shape.template.Change
import utopia.genesis.util.Extensions._
import utopia.genesis.util.{ApproximatelyEquatable, Arithmetic, Signed}

import scala.concurrent.duration.{Duration, TimeUnit}

object LinearVelocity
{
	/**
	  * A zero speed
	  */
	val zero = LinearVelocity(0, 1.seconds)
	
	/**
	  * @param amount Amount of distance travelled in 1 unit of time
	  * @param timeUnit Time unit used (implicit)
	  * @return A new velocity
	  */
	def apply(amount: Double)(implicit timeUnit: TimeUnit): LinearVelocity = new LinearVelocity(amount, Duration(1, timeUnit))
}

/**
  * Used for tracking speed but not direction
  * @author Mikko Hilpinen
  * @since 11.9.2019, v2.1+
  */
case class LinearVelocity(override val amount: Double, override val duration: Duration) extends
	Change[Double, LinearVelocity] with Arithmetic[LinearVelocity, LinearVelocity] with RichComparable[LinearVelocity]
	with Signed[LinearVelocity] with ApproximatelyEquatable[LinearVelocity]
{
	// COMPUTED	------------------------
	
	/**
	  * @return Whether this velocity has 0 amount and doesn't move items
	  */
	def isZero = amount == 0
	
	
	// IMPLEMENTED	--------------------
	
	override def repr = this
	
	override def *(mod: Double) = LinearVelocity(amount * mod, duration)
	
	override def toString = s"$perMilliSecond/ms"
	
	def +(another: LinearVelocity) = LinearVelocity(amount + another(duration), duration)
	
	override def compareTo(o: LinearVelocity) = perMilliSecond.compareTo(o.perMilliSecond)
	
	def -(another: LinearVelocity) = this + (-another)
	
	def isPositive = amount >= 0
	
	override protected def zero = LinearVelocity.zero
	
	override def ~==(other: LinearVelocity) = perMilliSecond ~== other.perMilliSecond
	
	
	// OTHER	------------------------
	
	/**
	  * @param vector A vector this velocity is multiplied with
	  * @return A velocity vector with the same direction as the specified vector's. If a unit vector was provided, the
	  *         linear velocity of the resulting velocity will match this velocity.
	  */
	def *(vector: Vector2D) = Velocity2D(vector * amount, duration)
	
	/**
	  * @param vector A vector this velocity is multiplied with
	  * @return A velocity vector with the same direction as the specified vector's. If a unit vector was provided, the
	  *         linear velocity of the resulting velocity will match this velocity.
	  */
	def *(vector: Vector3D) = Velocity3D(vector * amount, duration)
	
	/**
	  * Adds provided amount of velocity to this one. Will never change the sign of this velocity but will stop at 0
	  * @param amount The amount of velocity to add
	  * @return A combined amount of velocities (0 if this velocity would have changed sign)
	  */
	def increasePreservingDirection(amount: LinearVelocity) =
	{
		// Case: Applying change would change direction
		if (isPositive != amount.isPositive && abs <= amount.abs)
			LinearVelocity(0, duration)
		else
			this + amount
	}
	
	/**
	  * Subtracts provided amount of velocity from this one. Will never change the sign of this velocity but will stop at 0
	  * @param amount The amount of velocity to subtract
	  * @return Subtraction of these velocities (0 if this velocity would have changed sign)
	  */
	def decreasePreservingDirection(amount: LinearVelocity) = increasePreservingDirection(-amount)
	
	/**
	  * Adds direction component to this velocity
	  * @param direction Direction of this velocity
	  * @return A directional velocity
	  */
	def withDirection(direction: Angle) = Velocity2D(Vector2D.lenDir(amount, direction), duration)
	
	/**
	  * @param acceleration Acceleration applied
	  * @return The duration it takes to stop when specified acceleration is applied consistently.
	  *         None if this never happens.
	  */
	def durationUntilStopWith(acceleration: LinearAcceleration) =
		if (amount == 0) Some(Duration.Zero) else if (isPositive == acceleration.isPositive || acceleration.isZero) None else
			Some((perMilliSecond / acceleration.perMilliSecond.perMilliSecond).abs.millis)
	
	/**
	  * Calculates amount of transition over a period of time when consistent acceleration is also applied
	  * @param time Duration of transition
	  * @param acceleration Acceleration applied over the whole duration (expected to be consistent)
	  * @return Amount of transition in provided time period + the velocity at the end of this period
	  */
	def apply(time: Duration, acceleration: LinearAcceleration, preserveDirection: Boolean = false): (Double, LinearVelocity) =
	{
		// If preserving direction, has to check whether the movement would stop at a certain point
		val durationLimit =
		{
			if (preserveDirection)
				durationUntilStopWith(acceleration).filter(_ < time)
			else
				None
		}
		
		durationLimit match
		{
			case Some(limit) => apply(limit, acceleration)
			case None =>
				val endVelocity = this + acceleration(time)
				val averageVelocity = average(endVelocity)
				averageVelocity(time) -> endVelocity
		}
	}
}

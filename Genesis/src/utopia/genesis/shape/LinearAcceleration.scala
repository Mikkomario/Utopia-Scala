package utopia.genesis.shape

import utopia.flow.util.RichComparable
import utopia.genesis.util.{ApproximatelyEquatable, Arithmetic, Signed}
import utopia.flow.util.TimeExtensions._

import scala.concurrent.duration.{Duration, TimeUnit}

object LinearAcceleration
{
	/**
	  * A acceleration with 0 amount
	  */
	val zero = LinearAcceleration(LinearVelocity.zero, 1.seconds)
	
	/**
	  * @param velocityChange Amount of change in velocity in 1 unit of time
	  * @param timeUnit Time unit used (Eg. seconds&#94;2)
	  * @return A new acceleration
	  */
	def apply(velocityChange: Double)(implicit timeUnit: TimeUnit): LinearAcceleration = new LinearAcceleration(
		LinearVelocity(velocityChange), Duration(1, timeUnit))
}

/**
  * Used for tracking acceleration without direction
  * @author Mikko Hilpinen
  * @since 13.9.2019, v2.1+
  */
case class LinearAcceleration(override val amount: LinearVelocity, override val duration: Duration) extends
	Change[LinearVelocity, LinearAcceleration] with Arithmetic[LinearAcceleration, LinearAcceleration]
	with RichComparable[LinearAcceleration] with Signed[LinearAcceleration] with ApproximatelyEquatable[LinearAcceleration]
{
	// COMPUTED	-----------------------
	
	/**
	  * @return Whether this is a zero acceleration that doesn't actually affect velocity
	  */
	def isZero = amount.amount == 0
	
	
	// IMPLEMENTED	-------------------
	
	override def *(mod: Double) = LinearAcceleration(amount * mod, duration)
	
	override def +(another: LinearAcceleration) = LinearAcceleration(amount + another(duration), duration)
	
	override def -(another: LinearAcceleration) = this + (-another)
	
	override def toString = s"${perMilliSecond.amount}/ms^2"
	
	override def compareTo(o: LinearAcceleration) = perMilliSecond.compareTo(o.perMilliSecond)
	
	def isPositive = amount.isPositive
	
	override protected def zero = LinearAcceleration.zero
	
	override protected def self = this
	
	override def ~==(other: LinearAcceleration) = perMilliSecond ~== other.perMilliSecond
	
	
	// OTHER	-----------------------
	
	/**
	  * @param direction Target direction
	  * @return A directed version of this acceleration
	  */
	def withDirection(direction: Angle) = Acceleration(amount.withDirection(direction), duration)
}

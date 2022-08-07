package utopia.paradigm.motion.motion1d

import utopia.flow.operator.DoubleLike
import utopia.flow.time.TimeExtensions._
import utopia.flow.util.ApproximatelyEquatable
import utopia.paradigm.angular.Angle
import utopia.paradigm.motion.motion2d.Acceleration2D
import utopia.paradigm.motion.motion3d.Acceleration3D
import utopia.paradigm.motion.template.Change
import utopia.paradigm.shape.shape2d.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D

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
  * @since Genesis 13.9.2019, v2.1+
  */
case class LinearAcceleration(override val amount: LinearVelocity, override val duration: Duration)
	extends Change[LinearVelocity, LinearAcceleration] with DoubleLike[LinearAcceleration]
		with ApproximatelyEquatable[LinearAcceleration]
{
	// IMPLEMENTED	-------------------
	
	override def length = perMilliSecond.length
	
	/**
	  * @return Whether this is a zero acceleration that doesn't actually affect velocity
	  */
	override def isZero = amount.amount == 0
	
	override def repr = this
	
	override def *(mod: Double) = LinearAcceleration(amount * mod, duration)
	
	override def +(another: LinearAcceleration) = LinearAcceleration(amount + another(duration), duration)
	
	def -(another: LinearAcceleration) = this + (-another)
	
	override def toString = s"${perMilliSecond.amount}/ms^2"
	
	override def compareTo(o: LinearAcceleration) = perMilliSecond.compareTo(o.perMilliSecond)
	
	override def isPositive = if (duration >= Duration.Zero) amount.isPositive else amount.isNegative
	
	override protected def zero = LinearAcceleration.zero
	
	override def ~==(other: LinearAcceleration) = perMilliSecond ~== other.perMilliSecond
	
	
	// OTHER	-----------------------
	
	/**
	  * @param vector A vector
	  * @return Acceleration with the same direction as the specified vector. If a unit vector was provided, the linear
	  *         acceleration of the resulting acceleration will match this acceleration.
	  */
	def *(vector: Vector2D) = Acceleration2D(amount * vector, duration)
	
	/**
	  * @param vector A vector
	  * @return Acceleration with the same direction as the specified vector. If a unit vector was provided, the linear
	  *         acceleration of the resulting acceleration will match this acceleration.
	  */
	def *(vector: Vector3D) = Acceleration3D(amount * vector, duration)
	
	/**
	  * @param direction Target direction
	  * @return A directed version of this acceleration
	  */
	def withDirection(direction: Angle) = Acceleration2D(amount.withDirection(direction), duration)
}

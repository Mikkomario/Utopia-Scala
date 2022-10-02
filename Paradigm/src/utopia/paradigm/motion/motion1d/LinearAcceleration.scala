package utopia.paradigm.motion.motion1d

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.operator.{CanBeAboutZero, DoubleLike}
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.angular.Angle
import utopia.paradigm.generic.LinearAccelerationType
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.motion.motion2d.Acceleration2D
import utopia.paradigm.motion.motion3d.Acceleration3D
import utopia.paradigm.motion.template.{Change, ChangeFromModelFactory, ModelConvertibleChange}
import utopia.paradigm.shape.shape2d.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D

import scala.concurrent.duration.{Duration, TimeUnit}

object LinearAcceleration extends ChangeFromModelFactory[LinearAcceleration, LinearVelocity]
{
	/**
	  * A acceleration with 0 amount
	  */
	val zero = LinearAcceleration(LinearVelocity.zero, 1.seconds)
	
	override protected def amountFromValue(value: Value) = value.tryLinearVelocity
	
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
	extends ModelConvertibleChange[LinearVelocity, LinearAcceleration] with DoubleLike[LinearAcceleration]
		with CanBeAboutZero[Change[LinearVelocity, _], LinearAcceleration] with ValueConvertible
{
	// IMPLEMENTED	-------------------
	
	override def length = perMilliSecond.length
	
	override def isAboutZero = amount.isAboutZero || duration.isInfinite
	
	override def repr = this
	
	override implicit def toValue: Value = new Value(Some(this), LinearAccelerationType)
	
	override def *(mod: Double) = LinearAcceleration(amount * mod, duration)
	
	override def +(another: LinearAcceleration) = LinearAcceleration(amount + another(duration), duration)
	
	def -(another: LinearAcceleration) = this + (-another)
	
	override def toString = s"${perMilliSecond.amount}/ms^2"
	
	override def compareTo(o: LinearAcceleration) = perMilliSecond.compareTo(o.perMilliSecond)
	
	override def isPositive = if (duration >= Duration.Zero) amount.isPositive else amount.isNegative
	
	override def zero = LinearAcceleration.zero
	
	override def ~==(other: Change[LinearVelocity, _]) = perMilliSecond ~== other.perMilliSecond
	
	
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

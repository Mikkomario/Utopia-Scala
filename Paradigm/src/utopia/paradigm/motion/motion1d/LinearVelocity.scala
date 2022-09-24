package utopia.paradigm.motion.motion1d

import utopia.flow.collection.template.typeless
import utopia.flow.collection.value.typeless.ModelValidationFailedException
import utopia.flow.datastructure.immutable.Value
import utopia.flow.datastructure.template
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.factory.FromModelFactory
import utopia.flow.generic.model.immutable.{ModelValidationFailedException, Value}
import utopia.flow.generic.model.mutable.DoubleType
import utopia.flow.generic.model.template.{Model, ModelConvertible, Property, ValueConvertible}
import utopia.flow.operator.{ApproximatelyZeroable, DoubleLike}
import utopia.flow.time.TimeExtensions._
import utopia.flow.operator.EqualsExtensions._
import utopia.paradigm.angular.Angle
import utopia.paradigm.generic.LinearVelocityType
import utopia.paradigm.motion.motion2d.Velocity2D
import utopia.paradigm.motion.motion3d.Velocity3D
import utopia.paradigm.motion.template.Change
import utopia.paradigm.shape.shape2d.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D

import scala.concurrent.duration.{Duration, TimeUnit}
import scala.util.{Failure, Success}

object LinearVelocity extends FromModelFactory[LinearVelocity]
{
	// ATTRIBUTES   ---------------------------
	
	private val schema = ModelDeclaration("amount" -> DoubleType)
	
	/**
	  * A zero speed
	  */
	val zero: LinearVelocity = LinearVelocity(0, 1.seconds)
	
	
	// IMPLEMENTED  ---------------------------
	
	override def apply(model: Model[Property]) = schema.validate(model).toTry.flatMap { model =>
		val amount = model("amount").getDouble
		model("duration").duration match {
			case Some(duration) => Success(apply(amount, duration))
			case None =>
				if (amount ~== 0.0)
					Success(zero)
				else
					Failure(new ModelValidationFailedException(
						s"Required property 'duration' is missing. Specified properties: [${
							model.attributesWithValue.map { _.name }.mkString(", ") }]"))
		}
	}
	
	
	// OTHER    -------------------------------
	
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
  * @since Genesis 11.9.2019, v2.1+
  */
case class LinearVelocity(override val amount: Double, override val duration: Duration)
	extends Change[Double, LinearVelocity] with DoubleLike[LinearVelocity]
		with ApproximatelyZeroable[LinearVelocity, LinearVelocity] with ModelConvertible with ValueConvertible
{
	// IMPLEMENTED	--------------------
	
	override def length = perMilliSecond
	
	/**
	  * @return Whether this velocity has 0 amount and doesn't move items
	  */
	override def isZero = amount == 0.0 || duration.isInfinite
	override def isAboutZero = (amount ~== 0.0) || duration.isInfinite
	
	override def repr = this
	
	override def toString = s"$perMilliSecond/ms"
	
	// The amount and the duration may cancel each other out
	override def isPositive = nonZero && (if (amount > 0) duration >= Duration.Zero else duration < Duration.Zero)
	
	override protected def zero = LinearVelocity.zero
	
	override implicit def toValue: Value = new Value(Some(this), LinearVelocityType)
	
	override def toModel = duration.finite match {
		case Some(duration) => Model.from("amount" -> amount, "duration" -> duration)
		case None => Model.from("amount" -> 0.0)
	}
	
	override def +(another: LinearVelocity) = LinearVelocity(amount + another(duration), duration)
	
	override def compareTo(o: LinearVelocity) = perMilliSecond.compareTo(o.perMilliSecond)
	
	def -(another: LinearVelocity) = this + (-another)
	
	override def *(mod: Double) = LinearVelocity(amount * mod, duration)
	
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
				val averageVelocity = this.average(endVelocity)
				averageVelocity(time) -> endVelocity
		}
	}
}

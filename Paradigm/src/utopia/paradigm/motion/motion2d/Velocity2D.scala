package utopia.paradigm.motion.motion2d

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.generic.ParadigmDataType.Velocity2DType
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.motion.motion1d.LinearVelocity
import utopia.paradigm.motion.motion3d.Velocity3D
import utopia.paradigm.motion.template.{ChangeFromModelFactory, ModelConvertibleChange, VelocityLike}
import utopia.paradigm.shape.shape2d.Vector2D
import utopia.paradigm.shape.template.{Dimensions, DimensionsWrapperFactory, DoubleVector, HasDimensions}

import scala.concurrent.duration.{Duration, TimeUnit}

object Velocity2D
	extends DimensionsWrapperFactory[LinearVelocity, Velocity2D] with ChangeFromModelFactory[Velocity2D, Vector2D]
{
	// ATTRIBUTES   -------------------------
	
	/**
	  * A zero velocity
	  */
	val zero = Velocity2D(Vector2D.zero, 1.seconds)
	
	
	// IMPLEMENTED  -------------------------
	
	override def zeroDimension = LinearVelocity.zero
	
	override def apply(dimensions: Dimensions[LinearVelocity]) = {
		val duration = dimensions.x.duration
		apply(Vector2D(dimensions.map { _ over duration }), duration)
	}
	
	override def from(other: HasDimensions[LinearVelocity]) = other match {
		case v: Velocity2D => v
		case v3: Velocity3D => v3.in2D
		case o => apply(o.dimensions)
	}
	
	override protected def amountFromValue(value: Value) = value.tryVector2D
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param amount Distance vector traversed in 1 time unit
	  * @param timeUnit Time unit used (implicit)
	  * @return A new velocity
	  */
	def apply(amount: Vector2D)(implicit timeUnit: TimeUnit): Velocity2D = new Velocity2D(amount, Duration(1, timeUnit))
}

/**
  * Used for tracking speed in 2D space
  * @author Mikko Hilpinen
  * @since Genesis 14.7.2020, v2.3
  * @param transition The amount of transition over 'duration'
  * @param duration The duration of change
  */
case class Velocity2D(transition: Vector2D, override val duration: Duration)
	extends VelocityLike[Vector2D, Velocity2D] with ModelConvertibleChange[Vector2D, Velocity2D]
		with ValueConvertible
{
	// COMPUTED	-----------------
	
	/**
	  * @return A 3D copy of this velocity
	  */
	def in3D = Velocity3D(transition.toVector3D, duration)
	
	
	// IMPLEMENTED	-------------
	
	override def zero = Velocity2D.zero
	
	override def self = this
	
	override implicit def toValue: Value = new Value(Some(this), Velocity2DType)
	
	override protected def buildCopy(transition: Vector2D, duration: Duration) = copy(transition, duration)
	
	override def withDimensions(newDimensions: Dimensions[LinearVelocity]) =
		copy(Vector2D(newDimensions.map { _ over duration }))
	
	override def projectedOver(vector: DoubleVector) = Velocity2D(transition.projectedOver(vector), duration)
	
	
	// OTHER	-----------------
	
	/**
	  * @param time Target duration
	  * @return An acceleration to increase this amount of velocity in specified time
	  */
	def acceleratedIn(time: Duration) = Acceleration2D(this, time)
}

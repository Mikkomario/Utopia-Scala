package utopia.paradigm.motion.motion3d

import utopia.flow.generic.model.immutable.Value
import utopia.flow.generic.model.template.ValueConvertible
import utopia.flow.time.Duration
import utopia.flow.time.TimeUnit.Second
import utopia.paradigm.generic.ParadigmDataType.Velocity3DType
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.motion.motion1d.LinearVelocity
import utopia.paradigm.motion.motion2d.Velocity2D
import utopia.paradigm.motion.template.{ChangeFactory, ModelConvertibleChange, VelocityLike}
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.vector.DoubleVector
import utopia.paradigm.shape.template.{Dimensions, DimensionsWrapperFactory, HasDimensions}

object Velocity3D
	extends DimensionsWrapperFactory[LinearVelocity, Velocity3D] with ChangeFactory[Velocity3D, Vector3D]
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A zero velocity
	  */
	lazy val zero = apply(Vector3D.zero, Second)
	
	
	// IMPLEMENTED  -----------------------
	
	override def zeroDimension = LinearVelocity.zero
	
	override def apply(dimensions: Dimensions[LinearVelocity]) = {
		val duration = dimensions.x.duration
		apply(Vector3D(dimensions.map { _ over duration }), duration)
	}
	
	override def from(other: HasDimensions[LinearVelocity]) = other match {
		case v: Velocity3D => v
		case v: Velocity2D => v.in3D
		case o => apply(o.dimensions)
	}
	
	override protected def amountFromValue(value: Value) = value.tryVector3D
}

/**
  * Used for tracking speed in 3D space
  * @author Mikko Hilpinen
  * @since Genesis 14.7.2020, v2.3
  * @param transition The amount of transition over 'duration'
  * @param duration The duration of change
  */
case class Velocity3D(transition: Vector3D, override val duration: Duration)
	extends VelocityLike[Vector3D, Velocity3D] with ModelConvertibleChange[Vector3D, Velocity3D]
		with ValueConvertible
{
	// ATTRIBUTES   -------------
	
	// Caches the dimensions
	override lazy val dimensions = super.dimensions
	
	
	// COMPUTED	-----------------
	
	/**
	  * @return A copy of this velocity without z-axis movement
	  */
	def in2D = Velocity2D(transition.toVector2D, duration)
	
	
	// IMPLEMENTED	-------------
	
	override def zero = Velocity3D.zero
	
	override implicit def toValue: Value = new Value(Some(this), Velocity3DType)
	
	override def withDimensions(newDimensions: Dimensions[LinearVelocity]) =
		copy(Vector3D(newDimensions.map { _ over duration }))
	
	override protected def buildCopy(transition: Vector3D, duration: Duration) = copy(transition, duration)
	
	override def self = this
	
	override def projectedOver(vector: DoubleVector) = Velocity3D(transition.projectedOver(vector), duration)
	
	
	// OTHER	-----------------
	
	/**
	  * @param time Target duration
	  * @return An acceleration to increase this amount of velocity in specified time
	  */
	def acceleratedIn(time: Duration) = Acceleration3D(this, time)
}

package utopia.paradigm.motion.motion2d

import utopia.flow.generic.model.immutable.Value
import utopia.flow.time.Duration
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.paradigm.motion.motion3d.Acceleration3D
import utopia.paradigm.motion.template.{AccelerationFactory, AccelerationLike, ChangeFactory, ModelConvertibleChange}
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.template.vector.DoubleVector
import utopia.paradigm.shape.template.{Dimensions, DimensionsWrapperFactory, HasDimensions}

object Acceleration2D
	extends DimensionsWrapperFactory[LinearAcceleration, Acceleration2D]
		with AccelerationFactory[Acceleration2D, Velocity2D, Vector2D]
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A zero acceleration
	  */
	val zero = Acceleration2D(Velocity2D.zero, 1.seconds)
	
	
	// IMPLEMENTED  ------------------------
	
	override def zeroDimension = LinearAcceleration.zero
	
	override protected def velocityFactory: ChangeFactory[Velocity2D, Vector2D] = Velocity2D
	
	override def apply(dimensions: Dimensions[LinearAcceleration]) = {
		val duration = dimensions.x.duration
		apply(Velocity2D(dimensions.map { _ over duration }), duration)
	}
	override def from(other: HasDimensions[LinearAcceleration]) = other match {
		case a: Acceleration2D => a
		case a: Acceleration3D => a.in2D
		case o => apply(o.dimensions)
	}
	
	override protected def amountFromValue(value: Value) = value.tryVelocity2D
}

/**
  * Represents a change in velocity over a time period
  * @author Mikko Hilpinen
  * @since Genesis 14.7.2020, v2.3
  */
case class Acceleration2D(override val amount: Velocity2D, override val duration: Duration)
	extends AccelerationLike[Vector2D, Velocity2D, Acceleration2D]
		with ModelConvertibleChange[Velocity2D, Acceleration2D]
{
	// COMPUTED	-----------------------
	
	/**
	  * @return A 3D copy of this acceleration
	  */
	def in3D = Acceleration3D(amount.in3D, duration)
	
	
	// IMPLEMENTED	-------------------
	
	override def self = this
	override def zero = Acceleration2D.zero
	
	override def withDimensions(newDimensions: Dimensions[LinearAcceleration]) =
		copy(Velocity2D(newDimensions.map { _ over duration }))
	
	override protected def buildCopy(amount: Velocity2D, duration: Duration) = copy(amount, duration)
	
	override def projectedOver(vector: DoubleVector) = Acceleration2D(amount.projectedOver(vector), duration)
}

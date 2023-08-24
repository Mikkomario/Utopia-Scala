package utopia.paradigm.motion.motion3d

import utopia.flow.generic.model.immutable.Value
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.paradigm.motion.motion2d.Acceleration2D
import utopia.paradigm.motion.template.{AccelerationLike, ChangeFromModelFactory, ModelConvertibleChange}
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.{Dimensions, DimensionsWrapperFactory, DoubleVector, HasDimensions}

import scala.concurrent.duration.{Duration, TimeUnit}

object Acceleration3D extends DimensionsWrapperFactory[LinearAcceleration, Acceleration3D]
	with ChangeFromModelFactory[Acceleration3D, Velocity3D]
{
	/**
	  * A zero acceleration
	  */
	val zero = Acceleration3D(Velocity3D.zero, 1.seconds)
	
	override def zeroDimension = LinearAcceleration.zero
	
	override def apply(dimensions: Dimensions[LinearAcceleration]) = {
		val d = dimensions.x.duration
		apply(Velocity3D(dimensions.map { _ over d }), d)
	}
	
	override def from(other: HasDimensions[LinearAcceleration]) = other match {
		case a: Acceleration3D => a
		case a: Acceleration2D => a.in3D
		case o => apply(o.dimensions)
	}
	
	override protected def amountFromValue(value: Value) = value.tryVelocity3D
	
	/**
	  * @param velocityChange Amount of velocity change in 1 time unit
	  * @param timeUnit Time unit used (implicit)
	  * @return A new acceleration
	  */
	def apply(velocityChange: Vector3D)(implicit timeUnit: TimeUnit): Acceleration3D =
		new Acceleration3D(Velocity3D(velocityChange), Duration(1, timeUnit))
}

/**
  * Represents a change in velocity over a time period
  * @author Mikko Hilpinen
  * @since Genesis 14.7.2020, v2.3
  */
case class Acceleration3D(override val amount: Velocity3D, override val duration: Duration) extends
	AccelerationLike[Vector3D, Velocity3D, Acceleration3D] with ModelConvertibleChange[Velocity3D, Acceleration3D]
{
	// ATTRIBUTES   -------------------
	
	// Caches dimensions
	override lazy val dimensions = super.dimensions
	
	
	// COMPUTED -----------------------
	
	/**
	  * @return A 2D copy of this acceleration (where z-acceleration is 0)
	  */
	def in2D = Acceleration2D(amount.in2D, duration)
	
	
	// IMPLEMENTED	-------------------
	
	override def zero = Acceleration3D.zero
	
	override def self = this
	
	override def withDimensions(newDimensions: Dimensions[LinearAcceleration]) =
		copy(Velocity3D(newDimensions.map { _ over duration }))
	
	override protected def buildCopy(amount: Velocity3D, duration: Duration) = copy(amount, duration)
	
	override def projectedOver(vector: DoubleVector) = Acceleration3D(amount.projectedOver(vector), duration)
}

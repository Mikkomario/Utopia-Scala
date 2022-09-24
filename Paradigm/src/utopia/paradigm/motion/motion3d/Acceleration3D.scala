package utopia.paradigm.motion.motion3d

import utopia.flow.generic.model.immutable.Value
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.paradigm.motion.motion2d.Acceleration2D
import utopia.paradigm.motion.template.{AccelerationLike, ChangeFromModelFactory, ModelConvertibleChange}
import utopia.paradigm.shape.shape3d.{ThreeDimensional, Vector3D}
import utopia.paradigm.shape.template.VectorLike

import scala.concurrent.duration.{Duration, TimeUnit}

object Acceleration3D extends ChangeFromModelFactory[Acceleration3D, Velocity3D]
{
	/**
	  * A zero acceleration
	  */
	val zero = Acceleration3D(Velocity3D.zero, 1.seconds)
	
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
	AccelerationLike[Vector3D, Velocity3D, Acceleration3D] with ThreeDimensional[LinearAcceleration]
	with ModelConvertibleChange[Velocity3D, Acceleration3D]
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
	
	override def repr = this
	
	override protected def zeroAmount = Velocity3D.zero
	
	override protected def buildCopy(amount: Velocity3D, duration: Duration) = copy(amount, duration)
	
	override def projectedOver[V <: VectorLike[V]](vector: VectorLike[V]) =
		Acceleration3D(amount.projectedOver(vector), duration)
}

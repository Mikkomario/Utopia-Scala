package utopia.paradigm.motion.motion2d

import utopia.flow.datastructure.immutable.Value
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.paradigm.motion.motion3d.Acceleration3D
import utopia.paradigm.motion.template.{AccelerationLike, ChangeFromModelFactory, ModelConvertibleChange}
import utopia.paradigm.shape.shape2d.{TwoDimensional, Vector2D}
import utopia.paradigm.shape.template.VectorLike

import scala.concurrent.duration.{Duration, TimeUnit}

object Acceleration2D extends ChangeFromModelFactory[Acceleration2D, Velocity2D]
{
	/**
	  * A zero acceleration
	  */
	val zero = Acceleration2D(Velocity2D.zero, 1.seconds)
	
	override protected def amountFromValue(value: Value) = value.tryVelocity2D
	
	/**
	  * @param velocityChange Amount of velocity change in 1 time unit
	  * @param timeUnit Time unit used (implicit)
	  * @return A new acceleration
	  */
	def apply(velocityChange: Vector2D)(implicit timeUnit: TimeUnit): Acceleration2D =
		new Acceleration2D(Velocity2D(velocityChange), Duration(1, timeUnit))
}

/**
  * Represents a change in velocity over a time period
  * @author Mikko Hilpinen
  * @since Genesis 14.7.2020, v2.3
  */
case class Acceleration2D(override val amount: Velocity2D, override val duration: Duration) extends
	AccelerationLike[Vector2D, Velocity2D, Acceleration2D] with TwoDimensional[LinearAcceleration]
	with ModelConvertibleChange[Velocity2D, Acceleration2D]
{
	// ATTRIBUTES   -------------------
	
	override val dimensions2D = amount.dimensions2D.map { LinearAcceleration(_, duration) }
	
	
	// COMPUTED	-----------------------
	
	/**
	  * @return A 3D copy of this acceleration
	  */
	def in3D = Acceleration3D(amount.in3D, duration)
	
	
	// IMPLEMENTED	-------------------
	
	override def repr = this
	
	override protected def zeroAmount = Velocity2D.zero
	
	override protected def buildCopy(amount: Velocity2D, duration: Duration) = copy(amount, duration)
	
	override def projectedOver[V <: VectorLike[V]](vector: VectorLike[V]) =
		Acceleration2D(amount.projectedOver(vector), duration)
}

package utopia.paradigm.motion.motion3d

import utopia.flow.collection.value.typeless.Value
import utopia.flow.generic.ValueConvertible
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.generic.Velocity3DType
import utopia.paradigm.generic.ParadigmValue._
import utopia.paradigm.motion.motion1d.LinearVelocity
import utopia.paradigm.motion.motion2d.Velocity2D
import utopia.paradigm.motion.template.{ChangeFromModelFactory, ModelConvertibleChange, VelocityLike}
import utopia.paradigm.shape.shape3d.{ThreeDimensional, Vector3D}
import utopia.paradigm.shape.template.{Dimensional, VectorLike}

import scala.concurrent.duration.{Duration, TimeUnit}

object Velocity3D extends ChangeFromModelFactory[Velocity3D, Vector3D]
{
	// ATTRIBUTES   ------------------------
	
	/**
	  * A zero velocity
	  */
	val zero = Velocity3D(Vector3D.zero, 1.seconds)
	
	
	// IMPLEMENTED  -----------------------
	
	override protected def amountFromValue(value: Value) = value.tryVector3D
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param amount Distance vector traversed in 1 time unit
	  * @param timeUnit Time unit used (implicit)
	  * @return A new velocity
	  */
	def apply(amount: Vector3D)(implicit timeUnit: TimeUnit): Velocity3D = new Velocity3D(amount, Duration(1, timeUnit))
	
	/**
	  * @param amount Distance vector traversed in 1 time unit
	  * @param timeUnit Time unit used (implicit)
	  * @return A new velocity
	  */
	def apply(amount: Dimensional[Double])(implicit timeUnit: TimeUnit): Velocity3D =
		apply(Vector3D.withDimensions(amount.dimensions))
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
		with ThreeDimensional[LinearVelocity] with ValueConvertible
{
	// ATTRIBUTES   -------------
	
	// Caches the dimensions
	override lazy val dimensions = super.dimensions
	
	
	// COMPUTED	-----------------
	
	/**
	  * @return A copy of this velocity without z-axis movement
	  */
	def in2D = Velocity2D(transition.in2D, duration)
	
	
	// IMPLEMENTED	-------------
	
	override protected def zeroTransition = Vector3D.zero
	override protected def zeroAmount = zeroTransition
	
	override implicit def toValue: Value = new Value(Some(this), Velocity3DType)
	
	override protected def buildCopy(transition: Vector3D, duration: Duration) = copy(transition, duration)
	
	override def repr = this
	
	override def projectedOver[V <: VectorLike[V]](vector: VectorLike[V]) =
		Velocity3D(transition.projectedOver(vector), duration)
	
	
	// OTHER	-----------------
	
	/**
	  * @param time Target duration
	  * @return An acceleration to increase this amount of velocity in specified time
	  */
	def acceleratedIn(time: Duration) = Acceleration3D(this, time)
}

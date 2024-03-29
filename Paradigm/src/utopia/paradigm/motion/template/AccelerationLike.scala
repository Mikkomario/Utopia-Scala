package utopia.paradigm.motion.template

import utopia.flow.operator.MayBeAboutZero
import utopia.flow.operator.combine.Combinable
import utopia.flow.time.TimeExtensions._
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.motion.motion1d.LinearAcceleration
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.NumericVectorLike
import utopia.paradigm.shape.template.{Dimensional, VectorProjectable}

import scala.concurrent.duration.Duration

/**
  * Represents a change in velocity over a time period
  * @author Mikko Hilpinen
  * @since Genesis 14.7.2020, v2.3
  * @tparam X    Type of position information
  * @tparam V    Type of velocity information
  * @tparam Repr A concrete implementation of this trait
  */
trait AccelerationLike[X <: NumericVectorLike[Double, X, X], V <: VelocityLike[X, V], +Repr <: Change[V, _]]
	extends Change[V, Repr] with Combinable[Change[V, _], Repr] with Dimensional[LinearAcceleration, Repr]
		with VectorProjectable[Repr] with MayBeAboutZero[Change[Change[HasDoubleDimensions, _], _], Repr]
{
	// ABSTRACT	-----------------------
	
	/**
	  * @param amount   New acceleration amount (default = current)
	  * @param duration New acceleration duration (default = current)
	  * @return A new copy of this acceleration with specified values
	  */
	protected def buildCopy(amount: V = amount, duration: Duration = duration): Repr
	
	
	// COMPUTED	-----------------------
	
	override def dimensions = amount.dimensions.map { LinearAcceleration(_, duration) }
	
	/**
	  * @return Direction of this acceleration
	  */
	def direction = amount.direction
	
	/**
	  * @return This acceleration without the direction component
	  */
	def linear = LinearAcceleration(amount.linear, duration)
	
	
	// IMPLEMENTED	-------------------
	
	/**
	  * @return Whether this acceleration doesn't actually affect velocity in any way (is zero acceleration)
	  */
	override def isZero = amount.isZero || duration.isInfinite
	override def isAboutZero = amount.isAboutZero || duration.isInfinite
	
	override def ~==(other: Change[Change[HasDoubleDimensions, _], _]) =
		perMilliSecond ~== other.perMilliSecond
	
	override def *(mod: Double) = buildCopy(amount * mod)
	
	override def +(another: Change[V, _]) = buildCopy(amount + another(duration))
	
	override def toString = s"${ perMilliSecond.transition }/ms^2"
	
	override def apply(axis: Axis) = LinearAcceleration(amount(axis), duration)
	
	
	// OTHER	----------------------
	
	/**
	  * @param other Another acceleration without direction
	  * @return A sum of these accelerations. Has the same or opposite direction as this acceleration.
	  */
	def +(other: LinearAcceleration) = buildCopy(amount + other(duration))
	
	/**
	  * @param other Another acceleration without direction
	  * @return A subtraction of these accelerations. Has the same or opposite direction as this acceleration.
	  */
	def -(other: LinearAcceleration) = this + (-other)
	/**
	  * @param other Acceleration to subtract
	  * @return Difference between these accelerations
	  */
	def -(other: Change[V, _]) = buildCopy(amount - other(duration))
}

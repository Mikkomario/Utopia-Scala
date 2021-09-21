package utopia.genesis.shape.template

import utopia.flow.operator.Combinable
import utopia.genesis.shape.shape1D.LinearAcceleration
import utopia.genesis.shape.shape2D.Vector2DLike
import utopia.genesis.shape.{Axis, shape1D}

import scala.concurrent.duration.Duration

/**
  * Represents a change in velocity over a time period
  * @author Mikko Hilpinen
  * @since 14.7.2020, v2.3
  * @tparam X Type of position information
  * @tparam V Type of velocity information
  * @tparam Repr A concrete implementation of this trait
  */
trait AccelerationLike[X <: Vector2DLike[X], V <: VelocityLike[X, V],
	+Repr <: Change[V, _] /*with Arithmetic[Change[V, _], Repr]*/]
	extends Change[V, Repr] with Combinable[Repr, Change[V, _]] with Dimensional[LinearAcceleration]
		with VectorProjectable[Repr]
{
	// ABSTRACT	-----------------------
	
	/**
	  * @param amount New acceleration amount (default = current)
	  * @param duration New acceleration duration (default = current)
	  * @return A new copy of this acceleration with specified values
	  */
	protected def buildCopy(amount: V = amount, duration: Duration = duration): Repr
	
	
	// COMPUTED	-----------------------
	
	override def dimensions = amount.dimensions.map { shape1D.LinearAcceleration(_, duration) }
	
	override protected def zeroDimension = LinearAcceleration.zero
	
	/**
	  * @return Whether this acceleration doesn't actually affect velocity in any way (is zero acceleration)
	  */
	def isZero = amount.isZero
	
	/**
	  * @return Direction of this acceleration
	  */
	def direction = amount.direction
	
	/**
	  * @return This acceleration without the direction component
	  */
	def linear = LinearAcceleration(amount.linear, duration)
	
	
	// IMPLEMENTED	-------------------
	
	override def *(mod: Double) = buildCopy(amount * mod)
	
	override def +(another: Change[V, _]) = buildCopy(amount + another(duration))
	
	override def toString = s"${perMilliSecond.transition}/ms^2"
	
	override def along(axis: Axis) = shape1D.LinearAcceleration(amount.along(axis), duration)
	
	
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
}

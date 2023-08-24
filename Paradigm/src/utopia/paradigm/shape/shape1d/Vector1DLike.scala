package utopia.paradigm.shape.shape1d

import utopia.flow.operator.Sign.Positive
import utopia.flow.operator.{HasLength, Sign, SignOrZero, SignedOrZero}
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.enumeration.{Axis, Axis2D, Direction2D}
import utopia.paradigm.shape.template.{HasDimensions, NumericVectorLike}

/**
  * Common trait for one-dimensional vectors, i.e. lengths along individual axes
  *
  * @author Mikko Hilpinen
  * @since 24.8.2023, v1.4
  *
  * @tparam D Type of the length of this vector
  * @tparam Repr Implementing type of this vector
  * @tparam Transformed Type of this vector once a transformation or a direction-change function has been applied
  */
trait Vector1DLike[D, +Repr <: HasDimensions[D] with HasLength, +Transformed]
	extends Dimension[D] with NumericVectorLike[D, Repr, Transformed] with SignedOrZero[Repr]
{
	// ABSTRACT ------------------------------
	
	override protected def factory: Vector1DFactoryLike[D, Repr]
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return The direction of this vector on the 2D (X-Y) plane.
	  *         None if this vector is along the Z-axis.
	  */
	def direction2D = axis match {
		case axis: Axis2D => Some(Direction2D(axis, Sign.of(value).binaryOr(Positive)))
		case _ => None
	}
	
	/**
	  * @return A copy of this vector that points up (or down, if this vector has negative length)
	  */
	def up = towards(Up)
	/**
	  * @return A copy of this vector that points right (or left, if this vector has negative length)
	  */
	def right = towards(Direction2D.Right)
	/**
	  * @return A copy of this vector that points down (or up, if this vector has negative length)
	  */
	def down = towards(Down)
	/**
	  * @return A copy of this vector that points left (or right, if this vector has negative length)
	  */
	def left = towards(Direction2D.Left)
	
	
	// IMPLEMENTED  --------------------------
	
	override def zeroValue = n.zero
	
	override def sign: SignOrZero = Sign.of(value)
	override def isZero = value == n.zero
	override def isAboutZero = dimensionApproxEquals(value, n.zero)
	override def nonZero = !isZero
	
	override def zero = factory.zeroAlong(axis)
	override def toUnit = factory.unitAlong(axis)
	
	override def components = Vector(this)
	
	override def +(n: Double) = factory(this.n.plus(value, factory.dimensionFrom(n)), axis)
	override def -(n: Double) = this + (-n)
	override def *(n: D) = factory(this.n.times(value, n), axis)
	override def scaledBy(n: Double) = factory(factory.scale(value, n), axis)
	override def /(div: Double) = factory(factory.div(value, div), axis)
	
	override def isParallelWith(axis: Axis) = this.axis == axis
	override def isPerpendicularTo(axis: Axis) = this.axis != axis
	
	override def withLength(length: Double) = factory(factory.dimensionFrom(length), axis)
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param axis New axis to apply to this vector
	  * @return A copy of this vector that runs along the specified axis
	  */
	def withAxis(axis: Axis) = factory(value, axis)
	
	/**
	  * @param direction A 2D direction
	  * @return A copy of this vector that points towards the specified direction
	  *         (except if this vector had negative length,
	  *         in which case the resulting vector will point to the opposite direction).
	  */
	def towards(direction: Direction2D) = factory(direction.sign * value, direction.axis)
}

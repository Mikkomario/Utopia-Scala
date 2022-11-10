package utopia.paradigm.shape.shape1d

import utopia.flow.operator.EqualsExtensions._
import utopia.flow.operator.Sign
import utopia.paradigm.enumeration.Axis.{X, Y, Z}
import utopia.paradigm.enumeration.Direction2D.{Down, Up}
import utopia.paradigm.enumeration.{Axis, Axis2D, Direction2D}
import utopia.paradigm.shape.shape2d.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.{Dimensions, DoubleVector, DoubleVectorLike, HasDimensions, VectorFactory}

object Vector1D
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * A unit (1.0) vector (along the X-axis)
	  */
	val unit = apply(1.1)
	/**
	  * A zero vector (along the X-axis)
	  */
	val zero = apply(0.0)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param axis Axis along which this vector should run
	  * @return A unit vector along the specified axis
	  */
	def unitAlong(axis: Axis) = apply(1.0, axis)
	/**
	  * @param direction Direction of the resulting vector
	  * @return A unit vector pointing towards the specified direction
	  */
	def unitTowards(direction: Direction2D) = apply(direction.sign.modifier, direction.axis)
	
	/**
	  * @param axis Axis along which this vector should run
	  * @return A zero length vector along the specified axis
	  */
	def zeroAlong(axis: Axis) = apply(0.0, axis)
	
	
	// NESTED   -----------------------
	
	private object Factory extends VectorFactory[Vector1D]
	{
		override def apply(dimensions: Dimensions[Double]) = {
			dimensions.zipWithAxis.find { _._1 !~== 0.0 } match {
				case Some((length, axis)) => Vector1D(length, axis)
				case None => Vector1D.zero
			}
		}
		
		override def from(other: HasDimensions[Double]) = other match {
			case v: Vector1D => v
			case v: DoubleVectorLike[_] => v.components.find { _.nonZero }.getOrElse(zero)
			case o => apply(o.dimensions)
		}
	}
}

/**
  * Represents a one-dimensional vector, i.e. length along a specified axis
  * @author Mikko Hilpinen
  * @since 16.9.2022, v1.1
  */
case class Vector1D(override val length: Double, axis: Axis = X) extends DoubleVectorLike[Vector1D] with DoubleVector
{
	// ATTRIBUTES   --------------------------
	
	override lazy val dimensions = Dimensions.double(Map(axis -> length))
	
	
	// COMPUTED ------------------------------
	
	/**
	  * @return The direction of this vector on the 2D (X-Y) plane.
	  *         None if this vector is along the Z-axis.
	  */
	def direction2D = axis match {
		case axis: Axis2D => Some(Direction2D(axis, Sign.of(length)))
		case _ => None
	}
	
	/**
	  * @return A copy of this vector as a 3-dimensional vector
	  */
	def in3D = axis match {
		case X => Vector3D(length)
		case Y => Vector3D(0.0, length)
		case Z => Vector3D(0.0, 0.0, length)
	}
	/**
	  * @return A copy of this vector as a 2-dimensional vector along the X-Y-plane.
	  */
	def in2D = axis match {
		case X => Vector2D(length)
		case Y => Vector2D(0.0, length)
		case _ => Vector2D.zero
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
	
	override def zero = Vector1D.zeroAlong(axis)
	
	override def repr = this
	
	override protected def factory: VectorFactory[Vector1D] = Vector1D.Factory
	
	override def isZero = length == 0.0
	override def isAboutZero = length ~== 0.0
	
	override def toUnit = withLength(1.0)
	
	override def *(n: Double) = copy(length = length * n)
	override def +(length: Double) = copy(length = this.length + length)
	override def -(length: Double) = this + (-length)
	
	override def isParallelWith(axis: Axis) = this.axis == axis
	override def isPerpendicularTo(axis: Axis) = this.axis != axis
	
	override def withLength(length: Double) = copy(length)
	
	
	// OTHER    -----------------------------
	
	/**
	  * @param axis New axis to apply to this vector
	  * @return A copy of this vector that runs along the specified axis
	  */
	def withAxis(axis: Axis) = copy(axis = axis)
	
	/**
	  * @param direction A 2D direction
	  * @return A copy of this vector that points towards the specified direction
	  *         (except if this vector had negative length,
	  *         in which case the resulting vector will point to the opposite direction).
	  */
	def towards(direction: Direction2D) = copy(length * direction.sign.modifier, direction.axis)
}

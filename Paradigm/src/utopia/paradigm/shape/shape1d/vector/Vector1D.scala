package utopia.paradigm.shape.shape1d.vector

import utopia.flow.collection.immutable.Single
import utopia.flow.operator.equality.EqualsFunction
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.enumeration.Axis.{X, Y, Z}
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape3d.Vector3D
import utopia.paradigm.shape.template.vector.{DoubleVector, DoubleVectorFactory, DoubleVectorLike}
import utopia.paradigm.shape.template.{Dimensions, HasDimensions}

object Vector1D extends Vector1DFactoryLike[Double, Vector1D] with DoubleVectorFactory[Vector1D]
{
	// ATTRIBUTES   ----------------------
	
	/**
	  * A unit (1.0) vector (along the X-axis)
	  */
	override val unit = super.unit
	/**
	  * A zero vector (along the X-axis)
	  */
	override val zero = super.zero
	
	
	// NESTED   -----------------------
	
	override implicit def dimensionApproxEquals: EqualsFunction[Double] = EqualsFunction.approxDouble
	
	override def from(other: HasDimensions[Double]): Vector1D = other match {
		case v: Vector1D => v
		case v: DoubleVectorLike[_] => v.components.find { _.nonZero }.getOrElse(zero)
		case o => apply(o.dimensions)
	}
}

/**
  * Represents a one-dimensional vector, i.e. length along a specified axis
  * @author Mikko Hilpinen
  * @since 16.9.2022, v1.1
  */
case class Vector1D(override val length: Double, axis: Axis)
	extends Vector1DLike[Double, Vector1D, Vector1D] with DoubleVectorLike[Vector1D] with DoubleVector
{
	// ATTRIBUTES   --------------------------
	
	override lazy val dimensions = Dimensions.double(Map(axis -> length))
	
	
	// COMPUTED ------------------------------
	
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
	def in2D: Vector2D = axis match {
		case X => Vector2D(length)
		case Y => Vector2D(0.0, length)
		case _ => Vector2D.zero
	}
	
	
	// IMPLEMENTED  --------------------------
	
	override def self = this
	override protected def factory = Vector1D
	override def value = length
	
	override def components = Single(this)
	
	override def toUnit = factory(1.0, axis)
	
	override def +(n: Double) = factory(value + n, axis)
	override def -(n: Double) = factory(value - n, axis)
	
	override def withLength(length: Double) = factory(factory.dimensionFrom(length), axis)
}

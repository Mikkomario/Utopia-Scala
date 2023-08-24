package utopia.paradigm.shape.shape1d.rounding

import utopia.flow.operator.EqualsFunction
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.shape1d.{Vector1DFactoryLike, Vector1DLike}
import utopia.paradigm.shape.shape2d.rounding.RoundingVector2D
import utopia.paradigm.shape.template.{FromDimensionsFactory, HasDimensions, RoundingVector, RoundingVectorFactory, RoundingVectorLike}

object RoundingVector1D
	extends Vector1DFactoryLike[RoundingDouble, RoundingVector1D] with RoundingVectorFactory[RoundingVector1D]
{
	override lazy val unit = super.unit
	override lazy val zero = super.zero
	
	override implicit def dimensionApproxEquals: EqualsFunction[RoundingDouble] = RoundingDouble.equals
	
	override def from(other: HasDimensions[RoundingDouble]): RoundingVector1D = other match {
		case v: RoundingVector1D => v
		case v: RoundingVectorLike[_, _] => v.components.find { _.nonZero }.getOrElse(zero)
		case o => apply(o.dimensions)
	}
}

/**
  * A one-dimensional vector (i.e. length and direction) that uses rounding
  * @author Mikko Hilpinen
  * @since 24.8.2023, v1.4
  */
case class RoundingVector1D(value: RoundingDouble, axis: Axis)
	extends Vector1DLike[RoundingDouble, RoundingVector1D, RoundingVector2D]
		with RoundingVectorLike[RoundingVector1D, RoundingVector2D] with RoundingVector
{
	override def self: RoundingVector1D = this
	override protected def factory = RoundingVector1D
	override protected def fromDoublesFactory: FromDimensionsFactory[Double, RoundingVector2D] =
		RoundingVector2D.forDoubles
	
	override def components = Vector(this)
}
package utopia.paradigm.shape.shape1d.vector

import utopia.flow.operator.EqualsFunction
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.shape1d.RoundingDouble
import utopia.paradigm.shape.template.vector.{RoundingVector, RoundingVectorFactory, RoundingVectorLike}
import utopia.paradigm.shape.template.HasDimensions

object RoundingVector1D
	extends Vector1DFactoryLike[RoundingDouble, RoundingVector1D] with RoundingVectorFactory[RoundingVector1D]
{
	override lazy val unit = super.unit
	override lazy val zero = super.zero
	
	override implicit def dimensionApproxEquals: EqualsFunction[RoundingDouble] = RoundingDouble.equals
	
	override def from(other: HasDimensions[RoundingDouble]): RoundingVector1D = other match {
		case v: RoundingVector1D => v
		case v: RoundingVectorLike[_] => v.components.find { _.nonZero }.getOrElse(zero)
		case o => apply(o.dimensions)
	}
}

/**
  * A one-dimensional vector (i.e. length and direction) that uses rounding
  * @author Mikko Hilpinen
  * @since 24.8.2023, v1.4
  */
case class RoundingVector1D(value: RoundingDouble, axis: Axis)
	extends Vector1DLike[RoundingDouble, RoundingVector1D, RoundingVector1D]
		with RoundingVectorLike[RoundingVector1D] with RoundingVector
{
	override def self: RoundingVector1D = this
	override protected def factory = RoundingVector1D
	
	override def components = Vector(this)
	
	override def toUnit = super.toUnit
	
	override def +(n: Double) = factory(value + n, axis)
	override def -(n: Double) = factory(value - n, axis)
	
	override def withLength(length: Double) = factory(factory.dimensionFrom(length), axis)
}
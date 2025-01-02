package utopia.paradigm.shape.shape2d.vector.point

import utopia.flow.operator.equality.EqualsBy
import utopia.paradigm.measurement.{Distance, DistanceUnit}
import utopia.paradigm.shape.template.vector.{DistanceVector, DistanceVectorFactoryFactory, DistanceVectorLike}
import utopia.paradigm.shape.template.{Dimensions, FromDimensionsFactory, HasDimensions}

object RealPoint extends DistanceVectorFactoryFactory[RealPoint]
{
	// IMPLEMENTED  ----------------------
	
	override def apply(dimensions: Dimensions[Distance], defaultUnit: DistanceUnit): RealPoint =
		new RealPoint(dimensions.withLength(2), defaultUnit)
	override def from(other: HasDimensions[Distance], defaultUnit: DistanceUnit): RealPoint = other match {
		case p: RealPoint => p
		case d => apply(d.dimensions, defaultUnit)
	}
}

/**
  * Represents a location in a real-sized 2D space
  * @author Mikko Hilpinen
  * @since 02.01.2025, v1.7.1
  */
class RealPoint private(override val dimensions: Dimensions[Distance], defaultUnit: DistanceUnit)
	extends DistanceVector with DistanceVectorLike[RealPoint] with PointLike[Distance, RealPoint] with EqualsBy
{
	// ATTRIBUTES   ----------------------
	
	override protected lazy val factory = RealPoint(defaultUnit)
	
	
	// IMPLEMENTED  ----------------------
	
	override def self: RealPoint = this
	override protected def fromDoublesFactory: FromDimensionsFactory[Double, RealPoint] = factory.fromDoublesFactory
	
	override protected def equalsProperties: Seq[Any] = dimensions
}

package utopia.paradigm.shape.template.vector

import utopia.flow.operator.equality.EqualsBy
import utopia.paradigm.measurement.{Distance, DistanceUnit}
import utopia.paradigm.shape.template.{Dimensions, FromDimensionsFactory, HasDimensions}

import scala.language.implicitConversions

object DistanceVector extends DistanceVectorFactoryFactory[DistanceVector]
{
	// IMPLEMENTED  -------------------------
	
	override def apply(dimensions: Dimensions[Distance], defaultUnit: DistanceUnit): DistanceVector =
		new _DistanceVector(dimensions, defaultUnit)
	override def from(other: HasDimensions[Distance], defaultUnit: DistanceUnit): DistanceVector = other match {
		case d: DistanceVector => d
		case d => apply(d.dimensions, defaultUnit)
	}
	
	
	// NESTED   -----------------------------
	
	private class _DistanceVector(override val dimensions: Dimensions[Distance], defaultUnit: DistanceUnit)
		extends DistanceVector with EqualsBy
	{
		// ATTRIBUTES   --------------------
		
		override protected lazy val factory = DistanceVector(defaultUnit)
		
		
		// IMPLEMENTED  --------------------
		
		override def self: DistanceVector = this
		override protected def equalsProperties: Seq[Any] = dimensions
		
		override protected def fromDoublesFactory: FromDimensionsFactory[Double, DistanceVector] =
			factory.fromDoublesFactory
	}
}

/**
  * Common trait for distance-based vector implementations regardless of their number of applied dimensions
  * @author Mikko Hilpinen
  * @since 01.01.2025, v1.7.1
  */
trait DistanceVector extends DistanceVectorLike[DistanceVector]
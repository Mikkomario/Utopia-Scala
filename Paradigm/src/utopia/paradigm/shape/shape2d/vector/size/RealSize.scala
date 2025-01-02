package utopia.paradigm.shape.shape2d.vector.size

import utopia.flow.operator.equality.EqualsBy
import utopia.flow.operator.sign.Sign.{Negative, Positive}
import utopia.flow.operator.sign.SignOrZero.Neutral
import utopia.flow.operator.sign.{SignOrZero, SignedOrZero}
import utopia.paradigm.measurement.{Distance, DistanceUnit}
import utopia.paradigm.shape.template.vector.{DistanceVector, DistanceVectorFactoryFactory, DistanceVectorLike}
import utopia.paradigm.shape.template.{Dimensions, FromDimensionsFactory, HasDimensions}
import utopia.paradigm.transform.{Adjustment, SizeAdjustable}

object RealSize extends DistanceVectorFactoryFactory[RealSize]
{
	// IMPLEMENTED   ----------------------
	
	override def apply(dimensions: Dimensions[Distance], defaultUnit: DistanceUnit): RealSize =
		new RealSize(dimensions.withLength(2), defaultUnit)
	
	override def from(other: HasDimensions[Distance], defaultUnit: DistanceUnit): RealSize = other match {
		case s: RealSize => s
		case d => apply(d.dimensions, defaultUnit)
	}
}

/**
  * Represents a 2D real world size / area
  * @author Mikko Hilpinen
  * @since 01.01.2025, v1.7.1
  */
class RealSize private(override val dimensions: Dimensions[Distance], defaultUnit: DistanceUnit)
	extends DistanceVector with DistanceVectorLike[RealSize] with SizedLike[Distance, RealSize, RealSize]
		with SignedOrZero[RealSize] with SizeAdjustable[RealSize] with EqualsBy
{
	// ATTRIBUTES   ---------------------
	
	override protected lazy val factory = RealSize(defaultUnit)
	
	override lazy val sign: SignOrZero = {
		if (dimensions.exists { _.isZero })
			Neutral
		else if (dimensions.exists { _.isNegative })
			Negative
		else
			Positive
	}
	
	
	// IMPLEMENTED  ---------------------
	
	override def self: RealSize = this
	override def size: RealSize = this
	override protected def fromDoublesFactory: FromDimensionsFactory[Double, RealSize] = factory.fromDoublesFactory
	override protected def sizeFactory = factory
	
	override def width = x
	override def height = y
	
	override protected def equalsProperties: Seq[Any] = dimensions
	
	override def withSize(size: RealSize): RealSize = size
	
	override protected def adjustedBy(impact: Int)(implicit adjustment: Adjustment): RealSize =
		this * adjustment(impact)
}
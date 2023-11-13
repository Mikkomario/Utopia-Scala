package utopia.paradigm.shape.template.vector

import utopia.flow.operator.equality.EqualsExtensions._
import utopia.flow.operator.equality.EqualsFunction
import utopia.paradigm.shape.template.{Dimensions, HasDimensions}

object DoubleVector extends DoubleVectorFactory[DoubleVector]
{
	// IMPLICIT ------------------------------
	
	implicit val equals: EqualsFunction[DoubleVectorLike[_]] = _.dimensions ~== _.dimensions
	
	
	// IMPLEMENTED  --------------------------
	
	override def apply(dimensions: Dimensions[Double]): DoubleVector = _DoubleVector(dimensions)
	override def from(other: HasDimensions[Double]) = other match {
		case d: DoubleVector => d
		case o => apply(o.dimensions)
	}
	
	
	// NESTED   -----------------------------
	
	private case class _DoubleVector(dimensions: Dimensions[Double]) extends DoubleVector
	{
		override def self = this
		override protected def factory = DoubleVector
	}
}

/**
  * A common trait for vectors of doubles that are used in vector maths.
  * Classes extending this trait should also consider extending DoubleVectorLike with their own "Repr" -type
  * @author Mikko Hilpinen
  * @since 9.11.2022, v1.2
  */
trait DoubleVector extends DoubleVectorLike[DoubleVector]

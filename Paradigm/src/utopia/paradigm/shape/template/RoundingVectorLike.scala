package utopia.paradigm.shape.template

import utopia.flow.operator.{EqualsFunction, HasLength}
import utopia.paradigm.shape.shape1d.rounding.{RoundingDouble, RoundingVector1D}

/**
  * Common trait for sequences of rounded numbers that align with axes (X, Y, Z, ...)
  * @author Mikko Hilpinen
  * @since 24.8.2023, v1.4
  */
trait RoundingVectorLike[+Repr <: HasDimensions[RoundingDouble] with HasLength, +Transformed]
	extends NumericVectorLike[RoundingDouble, Repr, Transformed]
{
	// ABSTRACT -----------------------
	
	override protected def factory: RoundingVectorFactory[Repr]
	
	
	// IMPLEMENTED  -------------------
	
	override implicit def dimensionApproxEquals: EqualsFunction[RoundingDouble] = RoundingDouble.equals
	
	override def components: IndexedSeq[RoundingVector1D] =
		dimensions.zipWithAxis.map { case (d, axis) => RoundingVector1D(d, axis) }
}

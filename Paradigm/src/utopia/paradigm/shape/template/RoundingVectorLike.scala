package utopia.paradigm.shape.template

import utopia.flow.operator.{EqualsFunction, HasLength}
import utopia.paradigm.shape.shape1d.rounding.RoundingDouble

/**
  * Common trait for sequences of rounded numbers that align with axes (X, Y, Z, ...)
  * @author Mikko Hilpinen
  * @since 24.8.2023, v1.4
  */
trait RoundingVectorLike[+Repr <: HasDimensions[RoundingDouble] with HasLength]
	extends NumericVectorLike[RoundingDouble, Repr, Repr]
{
	// ABSTRACT -----------------------
	
	override protected def factory: RoundingVectorFactory[Repr]
	
	
	// IMPLEMENTED  -------------------
	
	override protected def fromDoublesFactory = factory.forDoubles
	override implicit def dimensionApproxEquals: EqualsFunction[RoundingDouble] = RoundingDouble.equals
}

package utopia.paradigm.shape.template.vector

import utopia.flow.operator.{EqualsFunction, HasLength}
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.shape1d.RoundingDouble
import utopia.paradigm.shape.shape1d.vector.RoundingVector1D
import utopia.paradigm.shape.template.{FromDimensionsFactory, HasDimensions}

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
	
	override implicit def dimensionApproxEquals: EqualsFunction[RoundingDouble] = RoundingDouble.equals
	override protected def fromDoublesFactory: FromDimensionsFactory[Double, Repr] = factory.forDoubles
	
	override def components: IndexedSeq[RoundingVector1D] =
		dimensions.zipWithAxis.map { case (d, axis) => RoundingVector1D(d, axis) }
	
	override def along(axis: Axis) = RoundingVector1D(apply(axis), axis)
	
	
	// OTHER    ----------------------
	
	def *(scaling: Double) = scaledBy(scaling)
	def /(div: Double) = dividedBy(div)
}

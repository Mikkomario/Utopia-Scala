package utopia.paradigm.shape.shape1d.range

import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.operator.equality.EqualsFunction
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.shape1d.RoundingDouble
import utopia.paradigm.shape.shape1d.vector.RoundingVector1D

object RoundingSpan1D extends Span1DFactoryLike[RoundingDouble, RoundingSpan1D]
{
	// ATTRIBUTES   ------------------------
	
	override val zero = super.zero
	
	private val zeroValue = NumericSpan.singleValue(RoundingDouble.zero)
	
	
	// IMPLEMENTED  ------------------------
	
	override implicit def n: Numeric[RoundingDouble] = RoundingDouble.numeric
	
	
	// OTHER    ---------------------------
	
	/**
	  * @param start The starting value (before rounding)
	  * @param end The ending value (before rounding)
	  * @param axis Axis along which this span runs
	  * @return A span along the specified axis with rounded values
	  */
	def apply(start: Double, end: Double, axis: Axis): RoundingSpan1D =
		apply(RoundingDouble(start), RoundingDouble(end), axis)
}

/**
  * A one-dimensional span between two rounded double values
  * @author Mikko Hilpinen
  * @since 24.8.2023, v1.4
  */
case class RoundingSpan1D(start: RoundingDouble, end: RoundingDouble, axis: Axis)
	extends Span1DLike[RoundingDouble, RoundingSpan1D, RoundingVector1D]
{
	// ATTRIBUTES   ------------------------
	
	override lazy val dimensions = super.dimensions
	override lazy val length = super.length
	
	
	// IMPLEMENTED  ------------------------
	
	override def n: Numeric[RoundingDouble] = RoundingSpan1D.n
	override implicit def lengthApproxEquals: EqualsFunction[RoundingDouble] = RoundingDouble.equals
	
	override protected def _self: RoundingSpan1D = this
	override def zero: RoundingSpan1D = RoundingSpan1D.zero
	override def zeroValue: NumericSpan[RoundingDouble] = RoundingSpan1D.zeroValue
	override def step: RoundingDouble = RoundingDouble.unit
	
	override protected def makeVector(len: RoundingDouble): RoundingVector1D = RoundingVector1D(len, axis)
}

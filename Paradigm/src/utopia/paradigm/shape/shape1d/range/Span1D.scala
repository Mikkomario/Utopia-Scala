package utopia.paradigm.shape.shape1d.range

import utopia.flow.collection.immutable.range.NumericSpan
import utopia.flow.operator.EqualsFunction
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.shape1d.vector.Vector1D

import scala.math.Numeric.DoubleIsFractional

object Span1D extends Span1DFactoryLike[Double, Span1D]
{
	// ATTRIBUTES   -----------------------
	
	/**
	  * A zero length span (along the x-axis)
	  */
	override lazy val zero = super.zero
	
	private lazy val zeroValue = NumericSpan(0.0, 0.0)
	
	
	// IMPLEMENTED  ---------------------
	
	override implicit def n: Numeric[Double] = DoubleIsFractional
}

/**
  * Represents a one-dimensional line or a segment
  * @author Mikko Hilpinen
  * @since 18.12.2022, v1.2
  */
case class Span1D(override val start: Double, override val end: Double, axis: Axis)
	extends Span1DLike[Double, Span1D, Vector1D]
{
	// ATTRIBUTES   ---------------------
	
	override lazy val length = super.length
	override lazy val dimensions = super.dimensions
	
	
	// IMPLEMENTED  ---------------------
	
	override protected def _self = this
	
	override def n = Span1D.n
	override implicit def lengthApproxEquals: EqualsFunction[Double] = EqualsFunction.approxDouble
	
	override def zero = Span1D.zeroAlong(axis)
	override def zeroValue = Span1D.zeroValue
	override def step = 1.0
	
	override protected def makeVector(len: Double): Vector1D = Vector1D(len, axis)
}

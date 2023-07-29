package utopia.paradigm.shape.shape1d

import utopia.flow.collection.immutable.range.{HasEnds, NumericSpan}
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.operator.{ApproxSelfEquals, CanBeAboutZero, EqualsFunction}
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.enumeration.Axis.X

object Span1D
{
	implicit val equalsFunction: EqualsFunction[Span1D] =
		EqualsFunction { (a, b) => a.toPair.~==(b.toPair)(EqualsFunction.approxDouble) }
	
	/**
	  * A zero length span (along the x-axis)
	  */
	lazy val zero = zeroAlong(X)
	
	private lazy val zeroValue = NumericSpan(0.0, 0.0)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param span A span
	  * @param axis Axis on which that span applies
	  * @return That span along the specified axis
	  */
	def apply(span: HasEnds[Double], axis: Axis): Span1D = apply(span.start, span.end, axis: Axis)
	
	/**
	  * @param axis An axis
	  * @return A zero length (0 to 0) span along the specified axis
	  */
	def zeroAlong(axis: Axis) = apply(0.0, 0.0, axis)
}

/**
  * Represents a one-dimensional line or a segment
  * @author Mikko Hilpinen
  * @since 18.12.2022, v1.2
  */
case class Span1D(override val start: Double, override val end: Double, axis: Axis)
	extends NumericSpan[Double] with Dimension[NumericSpan[Double]] with ApproxSelfEquals[Span1D]
		with CanBeAboutZero[Span1D, Span1D]
{
	// ATTRIBUTES   ---------------------
	
	override lazy val length = super.length
	override lazy val dimensions = super.dimensions
	
	
	// IMPLEMENTED  ---------------------
	
	override implicit def equalsFunction: EqualsFunction[Span1D] = Span1D.equalsFunction
	override def n = implicitly
	
	override def self = this
	override def value = this
	
	override def step = 1.0
	
	def vector = Vector1D(length, axis)
	def startVector = Vector1D(start, axis)
	def endVector = Vector1D(end, axis)
	
	override def isZero = start == 0.0 && end == 0.0
	override def nonZero = !isZero
	override def isAboutZero = (start ~== 0.0) && (end ~== 0.0)
	
	override def zeroValue = Span1D.zeroValue
	override def zero = Span1D.zeroAlong(axis)
}

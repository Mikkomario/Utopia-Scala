package utopia.paradigm.shape.shape1d.range

import utopia.flow.collection.immutable.range.{HasEnds, NumericSpan}
import utopia.flow.operator.EqualsExtensions._
import utopia.flow.operator.{ApproxEquals, CanBeAboutZero, EqualsFunction}
import utopia.paradigm.shape.shape1d.Dimension

/**
  * Common trait for one-dimensional lines and segments
  * @author Mikko Hilpinen
  * @since 24.8.2023, v1.4
  */
trait Span1DLike[D, +Repr <: NumericSpan[D], +V]
	extends NumericSpan[D] with Dimension[NumericSpan[D]] with ApproxEquals[Dimension[HasEnds[D]]]
		with CanBeAboutZero[Dimension[HasEnds[D]], Repr]
{
	// ABSTRACT -------------------------
	
	/**
	  * @return An approximate equality function applied for length values
	  */
	implicit def lengthApproxEquals: EqualsFunction[D]
	
	/**
	  * @return This instance
	  */
	protected def _self: Repr
	
	/**
	  * @param len Length
	  * @return A vector with the specified length along this span's axis
	  */
	protected def makeVector(len: D): V
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return This span as a vector
	  */
	def vector = makeVector(length)
	/**
	  * @return The start of this span as a vector
	  */
	def startVector = makeVector(start)
	/**
	  * @return The end of this span as a vector
	  */
	def endVector = makeVector(end)
	
	
	// IMPLEMENTED  ---------------------
	
	override def self = _self
	override def value = this
	
	override def isZero = start == n.zero && end == n.zero
	override def nonZero = !isZero
	override def isAboutZero = (start ~== n.zero) && (end ~== n.zero)
	
	override def ~==(other: Dimension[HasEnds[D]]): Boolean =
		axis == other.axis && ends.forallWith(other.value.ends) { _ ~== _ }
}

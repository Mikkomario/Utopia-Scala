package utopia.paradigm.shape.shape1d.range

import utopia.flow.collection.immutable.Pair
import utopia.flow.collection.immutable.range.HasEnds
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.enumeration.Axis.X

/**
  * Common trait for factories used for constructing spans (value ranges) that run along a specific axis.
  * @author Mikko Hilpinen
  * @since 24.8.2023
  * @tparam D Type of dimensions accepted by this factory
  * @tparam V Type of vectors produced by this factory
  */
trait Span1DFactoryLike[D, +V]
{
	// ABSTRACT -------------------------
	
	/**
	  * Creates a new span
	  * @param start The range start value (inclusive)
	  * @param end The range end value (inclusive)
	  * @param axis Axis along which this span runs
	  * @return A new span along the specified axis
	  */
	def apply(start: D, end: D, axis: Axis): V
	
	/**
	  * @return Numeric implementation for the used dimensions
	  */
	implicit def n: Numeric[D]
	
	
	// COMPUTED -----------------------
	
	/**
	  * A zero length span (along the x-axis)
	  */
	def zero = zeroAlong(X)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param span A span
	  * @param axis Axis on which that span applies
	  * @return That span along the specified axis
	  */
	def apply(span: Pair[D], axis: Axis): V = apply(span.first, span.second, axis)
	/**
	  * @param span A span
	  * @param axis Axis on which that span applies
	  * @return That span along the specified axis
	  */
	def apply(span: HasEnds[D], axis: Axis): V = apply(span.start, span.end, axis: Axis)
	
	/**
	  * @param axis An axis
	  * @return A zero length (0 to 0) span along the specified axis
	  */
	def zeroAlong(axis: Axis) = apply(n.zero, n.zero, axis)
}



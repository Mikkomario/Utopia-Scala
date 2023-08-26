package utopia.paradigm.shape.shape2d

import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.shape1d.Dimension
import utopia.paradigm.shape.template.HasDimensions

import scala.math.Ordered.orderingToOrdered

/**
  * A common trait for models / shapes that specify a size of some kind
  * @author Mikko Hilpinen
  * @since 25.8.2023, v1.4
  * @tparam D Type of dimensions used
  * @tparam S Type of size used
  */
trait HasSizeLike[D, +S <: HasDimensions[D]]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return Numeric implementation for the used dimensions type
	  */
	implicit def n: Numeric[D]
	
	/**
	  * @return The size of this shape
	  */
	def size: S
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return The width of this shape
	  */
	def width: D = size.x
	/**
	  * @return The height of this shape
	  */
	def height: D = size.y
	
	
	// OTHER    -------------------------
	
	/**
	  * @param axis Targeted axis
	  * @return The length of this item along the specified axis
	  */
	def lengthAlong(axis: Axis) = size(axis)
	
	/**
	  * @param area An area / size
	  * @return Whether this item completely fills an area of that size (i.e. is equal or larger on all axes)
	  */
	def fills(area: HasDimensions[D]) = size.forAllDimensionsWith(area) { _ >= _ }
	/**
	  * @param area An area / size
	  * @return Whether this item fits within an area of that size (i.e. is smaller or equal on all axes)
	  */
	def fitsWithin(area: HasDimensions[D]) = size.forAllDimensionsWith(area) { _ <= _ }
	
	/**
	  * @param v A one-dimensional vector
	  * @return Whether this item spans at least the area of that vector (along the axis of that vector)
	  */
	def spans(v: Dimension[D]) = size(v.axis) >= v.value
	/**
	  * @param v A one-dimensional vector
	  * @return Whether this item fits within the specified vector's length along the vector's axis
	  */
	def fitsWithin(v: Dimension[D]) = size(v.axis) <= v.value
}

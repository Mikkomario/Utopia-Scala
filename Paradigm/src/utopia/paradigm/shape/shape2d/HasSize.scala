package utopia.paradigm.shape.shape2d

import utopia.paradigm.enumeration.Axis
import utopia.paradigm.shape.shape1d.Vector1D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

/**
  * A common trait for models / shapes that specify a size
  * @author Mikko Hilpinen
  * @since 15.9.2022, v1.1
  */
trait HasSize
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return The size of this shape
	  */
	def size: Size
	
	
	// COMPUTED --------------------------
	
	/**
	  * @return The width of this shape
	  */
	def width: Double = size.x
	/**
	  * @return The height of this shape
	  */
	def height: Double = size.y
	
	
	// OTHER    -------------------------
	
	/**
	  * @param axis Targeted axis
	  * @return The length of this item along the specified axis
	  */
	def lengthAlong(axis: Axis) = size.along(axis)
	
	/**
	  * @param area An area / size
	  * @return Whether this item completely fills an area of that size (i.e. is equal or larger on all axes)
	  */
	def fills(area: HasDoubleDimensions) = size.forAllDimensionsWith(area) { _ >= _ }
	/**
	  * @param area An area / size
	  * @return Whether this item fits within an area of that size (i.e. is smaller or equal on all axes)
	  */
	def fitsWithin(area: HasDoubleDimensions) = size.forAllDimensionsWith(area) { _ <= _ }
	
	/**
	  * @param v A one-dimensional vector
	  * @return Whether this item spans at least the area of that vector (along the axis of that vector)
	  */
	def spans(v: Vector1D) = size.along(v.axis) >= v.length
	/**
	  * @param v A one-dimensional vector
	  * @return Whether this item fits within the specified vector's length along the vector's axis
	  */
	def fitsWithin(v: Vector1D) = size.along(v.axis) <= v.length
}

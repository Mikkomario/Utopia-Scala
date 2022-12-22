package utopia.paradigm.shape.shape2d

import utopia.flow.collection.immutable.range.HasEnds
import utopia.paradigm.enumeration.Axis
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.{DoubleVectorLike, HasDimensions}

/**
  * Common trait for shapes that can specify a bounding box
  * @author Mikko Hilpinen
  * @since Genesis 15.5.2021, v2.5.1
  */
trait HasBounds extends HasSize with Area2D
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Bounding box around this shape
	  */
	def bounds: Bounds
	
	
	// COMPUTED -------------------------
	
	/**
	  * @return The position of this item - typically the top-left corner of this item
	  */
	def position: Point = bounds.position
	
	/**
	  * @return The top-left corner of this item's bounds (assuming positive size)
	  */
	def topLeft = bounds.position
	/**
	  * @return The top-right corner of this item's bounds (assuming positive size)
	  */
	def topRight = topLeft + X(width)
	/**
	  * @return The bottom-left corner of this item's bounds (assuming positive size)
	  */
	def bottomLeft = topLeft + Y(height)
	/**
	  * @return The bottom-right corner of this item's bounds (assuming positive size)
	  */
	def bottomRight = Point(bounds.dimensions.map { _.end })
	
	/**
	  * @return A point at the center of this item vertically, at the left horizontally
	  */
	def centerLeft = topLeft + Y(height / 2)
	/**
	  * @return A point at the center of this item vertically, at the right horizontally
	  */
	def centerRight = topLeft + size * Vector2D(1, 0.5)
	/**
	  * @return A point at the center of this item horizontally, at the top vertically
	  */
	def centerTop = topLeft + X(width / 2)
	/**
	  * @return A point at the center of this item horizontally, at the bottom vertically
	  */
	def centerBottom = topLeft + size * Vector2D(0.5, 1)
	
	/**
	  * @return The leftmost x-coordinate of this item's bounds (assuming positive size)
	  */
	def leftX = minAlong(X)
	/**
	  * @return The rightmost x-coordinate of this item's bounds (assuming positive size)
	  */
	def rightX = maxAlong(X)
	/**
	  * @return The topmost y-coordinate of this item's bounds (assuming positive size)
	  */
	def topY = minAlong(Y)
	/**
	  * @return The bottom-most y-coordinate of this item's bounds (assuming positive size)
	  */
	def bottomY = maxAlong(Y)
	/**
	  * @return The center coordinate of this item along the x-axis
	  */
	def centerX = centerAlong(X)
	/**
	  * @return The center coordinate of this item along the y-axis
	  */
	def centerY = centerAlong(Y)
	
	
	// IMPLEMENTED  ---------------------
	
	override def size = bounds.size
	
	override def contains[V <: DoubleVectorLike[V]](point: V): Boolean = contains(point: HasDoubleDimensions)
	
	
	// OTHER    -------------------------
	
	/**
	  * @param point A point
	  * @return Whether this item's bounds contain that point (i.e. that point lies within said bounds)
	  */
	def contains(point: HasDoubleDimensions) = bounds.forAllDimensionsWith(point) { _ contains _ }
	
	/**
	  * @param axis The targeted axis
	  * @return The smallest coordinate within this item's bounds along the specified axis (assuming positive size)
	  */
	def minAlong(axis: Axis) = bounds(axis).start
	/**
	  * @param axis The targeted axis
	  * @return The largest coordinate within this item's bounds along the specified axis (assuming positive size)
	  */
	def maxAlong(axis: Axis) = bounds(axis).end
	/**
	  * @param axis Targeted axis
	  * @return The center coordinate of this item along that axis
	  */
	def centerAlong(axis: Axis) = {
		val range = bounds(axis)
		(range.end + range.start) / 2
	}
	
	/**
	  * @param bounds A set of bounds
	  * @return Whether this item's bounds overlap with the other set of bounds
	  */
	def overlapsWith(bounds: HasDimensions[HasEnds[Double]]) =
		this.bounds.forAllDimensionsWith(bounds) { _ overlapsWith _ }
	/**
	  * @param bounds A set of bounds
	  * @return Whether this item lies completely within the specified set of bounds
	  */
	def liesCompletelyWithin(bounds: HasDimensions[HasEnds[Double]]) =
		this.bounds.forAllDimensionsWith(bounds) { (my, their) => their.contains(my) }
}

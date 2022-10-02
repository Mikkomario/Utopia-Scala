package utopia.paradigm.shape.shape2d

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.{Axis, Axis2D}

/**
  * Common trait for shapes that can specify a bounding box
  * @author Mikko Hilpinen
  * @since Genesis 15.5.2021, v2.5.1
  */
trait Bounded extends Sized with Area2D
{
	// ABSTRACT --------------------------
	
	/**
	  * @return Bounding box around this shape
	  */
	def bounds: Bounds
	
	
	// COMPUTED -------------------------
	
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
	def bottomRight = topLeft + size
	/**
	  * @return The leftmost x-coordinate of this item's bounds (assuming positive size)
	  */
	def leftX = topLeft.x
	/**
	  * @return The rightmost x-coordinate of this item's bounds (assuming positive size)
	  */
	def rightX = leftX + width
	/**
	  * @return The topmost y-coordinate of this item's bounds (assuming positive size)
	  */
	def topY = topLeft.y
	/**
	  * @return The bottom-most y-coordinate of this item's bounds (assuming positive size)
	  */
	def bottomY = topY + height
	
	
	// IMPLEMENTED  ---------------------
	
	override def size = bounds.size
	
	override def contains[V <: Vector2DLike[V]](point: V) =
		point.x >= leftX && point.y >= topY && point.x <= rightX && point.y <= bottomY
	
	
	// OTHER    -------------------------
	
	/**
	  * @param axis The targeted axis
	  * @return The smallest coordinate within this item's bounds along the specified axis (assuming positive size)
	  */
	def minAlong(axis: Axis) = topLeft.along(axis)
	/**
	  * @param axis The targeted axis
	  * @return The largesst coordinate within this item's bounds along the specified axis (assuming positive size)
	  */
	def maxAlong(axis: Axis) = topLeft.along(axis) + size.along(axis)
	
	/**
	  * @param bounds A set of bounds
	  * @return Whether this item's bounds overlap with the other set of bounds
	  */
	def overlapsWith(bounds: Bounds) =
		Axis2D.values.forall { axis =>
			maxAlong(axis) > bounds.minAlong(axis) && bounds.maxAlong(axis) > minAlong(axis)
		}
	
	/**
	  * @param bounds A set of bounds
	  * @return Whether this item lies completely within the specified set of bounds
	  */
	def liesCompletelyWithin(bounds: Bounds) = bounds.contains(topLeft) && bounds.contains(topRight)
}

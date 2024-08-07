package utopia.paradigm.shape.shape2d.area.polygon.c4.bounds

import utopia.flow.collection.immutable.range.HasInclusiveEnds
import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.shape.shape2d.insets.Insets
import utopia.paradigm.shape.shape2d.vector.Vector2D
import utopia.paradigm.shape.shape2d.vector.point.Point
import utopia.paradigm.shape.shape2d.vector.size.{Size, Sized}
import utopia.paradigm.shape.template.HasDimensions
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions
import utopia.paradigm.shape.template.vector.DoubleVectorLike

/**
  * Common trait for shapes that can specify a bounding box
  * @author Mikko Hilpinen
  * @since Genesis 15.5.2021, v2.5.1
  */
trait Bounded[+Repr] extends HasBounds with Sized[Repr]
{
	// ABSTRACT --------------------------
	
	/**
	  * Creates a copy of this item with modified bounds
	  * @param newBounds Bounds for the new item
	  * @return A copy of this item with the specified bounds
	  */
	def withBounds(newBounds: Bounds): Repr
	
	
	// IMPLEMENTED  ---------------------
	
	override def withSize(size: Size) = withBounds(Bounds(topLeft, size))
	
	
	// OTHER    -------------------------
	
	/**
	  * @param f A mapping function for this item's bounds
	  * @return A copy of this item with mapped bounds
	  */
	def mapBounds(f: Bounds => Bounds) = withBounds(f(bounds))
	
	/**
	  * @param position A new (top-left) position to assign to this item
	  * @return A copy of this item moved to the specified position
	  */
	def withPosition(position: Point): Repr = withBounds(bounds.withPosition(position))
	
	/**
	  * Moves this item so that it's top-left coordinate is at the specified location
	  * @param topLeft New top-left coordinate for this item
	  * @param stretch Whether this item should be stretched so that the bottom-right corner is preserved (default = false)
	  * @return A copy of this item where the top-left coordinate is at the specified location
	  */
	def withTopLeft(topLeft: Point, stretch: Boolean = false) = {
		if (stretch)
			withBounds(Bounds(topLeft, size + (this.topLeft - topLeft)))
		else
			withBounds(Bounds(topLeft, size))
	}
	/**
	  * Moves this item so that it's top-right coordinate is at the specified location
	  * @param topRight New top-right coordinate for this item
	  * @param stretch Whether this item should be stretched so that the bottom-left corner is preserved (default = false)
	  * @return A copy of this item where the top-right coordinate is at the specified location
	  */
	def withTopRight(topRight: Point, stretch: Boolean = false) = {
		if (stretch)
			withBounds(Bounds(topLeft.withY(topRight.y), size + Vector2D(topRight.x - rightX, topY - topRight.y)))
		else
			withBounds(Bounds(topRight - X(width), size))
	}
	/**
	  * Moves this item so that it's bottom-left coordinate is at the specified location
	  * @param bottomLeft New bottom-left coordinate for this item
	  * @param stretch Whether this item should be stretched so that the top-right corner is preserved (default = false)
	  * @return A copy of this item where the bottom-left coordinate is at the specified location
	  */
	def withBottomLeft(bottomLeft: Point, stretch: Boolean = false) = {
		if (stretch)
			withBounds(Bounds(bottomLeft.withY(topY), size + Vector2D(leftX - bottomLeft.x, bottomLeft.y - bottomY)))
		else
			withBounds(Bounds(bottomLeft - Y(height), size))
	}
	/**
	  * Moves this item so that it's bottom-right coordinate is at the specified location
	  * @param bottomRight New bottom-right coordinate for this item
	  * @param stretch Whether this item should be stretched so that the top-left corner is preserved (default = false)
	  * @return A copy of this item where the bottom-right coordinate is at the specified location
	  */
	def withBottomRight(bottomRight: Point, stretch: Boolean = false) = {
		if (stretch)
			withBounds(Bounds(topLeft, size + (bottomRight - this.bottomRight)))
		else
			withBounds(Bounds(bottomRight - size, size))
	}
	/**
	  * @param center A new center point to assign
	  * @return Copy of this item with the center placed at the specified point
	  */
	def withCenter(center: Point) = translated(center - bounds.center)
	
	/**
	  * Creates a copy of this item that has been positioned within the specified bounds using a specific alignment.
	  * Doesn't modify the size of this item at all by default, even when it would not fit the specified set of bounds.
	  * However, if 'cropToFit' is set to true, the size of this item may be reduced to fit the specified bounds.
	  * This reduction doesn't preserve the shape of this item, however.
	  *
	  * If you wish to place this item completely within a set of bounds while also preserving shape, please use
	  * fittedWithin(Bounds, Alignment) instead.
	  * @param bounds A set of bounds
	  * @param alignment Alignment to use when positioning this item within the specified bounds.
	  *                  For example, if left alignment is used, this item will be located at the center-left position
	  *                  of the specified bounds.
	  * @param cropToFit whether the size of this item may be adjusted in order to fit within the specified area.
	  *                  Default = false.
	  * @return A relocated copy of this item
	  */
	def positionedWithin(bounds: Bounds, alignment: Alignment, cropToFit: Boolean = false) = {
		if (cropToFit) {
			val newSize = size.croppedToFitWithin(bounds.size)
			withBounds(alignment.position(newSize, bounds))
		}
		else
			withBounds(alignment.position(size, bounds))
	}
	
	/**
	  * Creates a copy of this item that has been positioned and possibly scaled to fit within the specified bounds,
	  * using the specified alignment. Preserves shape.
	  * @param bounds A set of bounds
	  * @param alignment Alignment to use when positioning this item within the specified bounds.
	  *                  For example, if left alignment is used, this item will be located at the center-left position
	  *                  of the specified bounds.
	  * @return A copy of this item that fits within the specified set of bounds
	  */
	def fittedWithin(bounds: Bounds, alignment: Alignment) = {
		val newSize = size.fittingWithin(bounds.size)
		withBounds(alignment.position(newSize, bounds))
	}
	
	/**
	  * Repositions this item so that it lies within the specified set of bounds.
	  * The applied movement is kept to a minimum. Will never alter the size of this item.
	  * If this item doesn't fit into the specified bounds,
	  * this item is positioned so that it covers as much of the specified area as possible
	  * @param area Target area
	  * @return A copy of this item so that it lies within that specified area (if possible)
	  */
	def shiftedInto(area: HasDimensions[HasInclusiveEnds[Double]]) =
		withBounds(bounds.mergeWith(area) { _ shiftedInto _ })
	/**
	  * Repositions and downscales this item so that it lies within the specified set of bounds.
	  * Size is altered only if this item wouldn't otherwise fit within the specified bounds.
	  * Movement is also minimized.
	  * @param area An area within which this item shall reside
	  * @return A copy of this item that lies completely within the specified area
	  */
	def fittedInto(area: HasDimensions[HasInclusiveEnds[Double]]) =
		withBounds(bounds.mergeWith(area) { (my, into) =>
			val maxLength = into.end - into.start
			if (my.length >= maxLength)
				my.withEnds(into.start, into.end)
			else if (my.start < into.start)
				my.withEnds(into.start, into.start + my.length)
			else if (my.end > into.end)
				my.withEnds(into.end - my.length, into.end)
			else
				my
		})
	
	/**
	  * Creates a copy of this item with scaled bounds (both size AND position). Preserves shape.
	  * @param scaling A scaling modifier to apply to the bounds of this item
	  * @return A scaled copy of this item
	  */
	def withScaledBounds(scaling: Double) = withBounds(bounds * scaling)
	/**
	  * Creates a copy of this item with scaled bounds (both size AND position). Doesn't preserve shape.
	  * @param scaling A scaling modifier to apply to the bounds of this item (different for different axes)
	  * @return A scaled copy of this item
	  */
	def withScaledBounds(scaling: HasDoubleDimensions) = withBounds(bounds * scaling)
	
	/**
	  * @param translation Translation to apply to this item's position
	  * @return A copy of this item with translated position
	  */
	def translated(translation: HasDoubleDimensions) = withBounds(bounds + translation)
	
	/**
	  * @param insets Insets to apply to this item's bounds
	  * @return A copy of this item with the insets added (both position and size are modified)
	  */
	def withInsets(insets: Insets) = withBounds(bounds + insets)
	/**
	  * Creates a copy of this item with enlarged bounds where the center-point remains the same
	  * @param enlargement A size increase to apply
	  * @return A copy of this item with bounds that keep the same center-point but have enlarged size
	  */
	def enlarged[V <: DoubleVectorLike[V]](enlargement: V) =
		withBounds(bounds.mergeWith(enlargement) { (area, increase) =>
			val halved = increase / 2
			area.withEnds(area.start - halved, area.end + halved)
		})
	/**
	  * @param enlargement Amount of enlargement **on each side**
	  * @return Copy of this instance with enlarged bounds
	  */
	def enlarged(enlargement: Double): Repr = enlarged(Vector2D.fill(2)(enlargement * 2))
	/**
	  * Creates a copy of this item with shrunk bounds where the center-point remains the same
	  * @param shrinking A size decrease to apply
	  * @return A copy of this item with bounds that keep the same center-point but have shrunk size
	  */
	def shrunk[V <: DoubleVectorLike[V]](shrinking: V) =
		withBounds(bounds.mergeWith(shrinking) { (area, decrease) =>
			// Won't shrink below length 0
			val halved = (decrease min area.length) / 2
			area.withEnds(area.start + halved, area.end - halved)
		})
	/**
	  * Creates a copy of this item with shrunk bounds where the center-point remains the same
	  * @param shrinking A size decrease to apply **on each side**
	  * @return A copy of this item with bounds that keep the same center-point but have shrunk size
	  */
	def shrunk(shrinking: Double): Repr = shrunk(Vector2D.fill(2)(shrinking * 2))
}

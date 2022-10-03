package utopia.paradigm.shape.shape2d

import utopia.paradigm.enumeration.Alignment
import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.shape.template.VectorLike.V
import utopia.paradigm.shape.template.{Dimensional, VectorLike}

/**
  * Common trait for shapes that can specify a bounding box
  * @author Mikko Hilpinen
  * @since Genesis 15.5.2021, v2.5.1
  */
trait BoundedLike[+Repr] extends Bounded with SizedLike[Repr]
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
	  * @return A relocated copy of this item
	  */
	def positionedWithin(bounds: Bounds, alignment: Alignment, cropToFit: Boolean = false) = {
		if (cropToFit) {
			val newSize = size.croppedToFitWithin(bounds.size)
			withBounds(Bounds(alignment.position(newSize, bounds), newSize))
		}
		else
			withTopLeft(alignment.position(size, bounds))
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
		val newTopLeft = alignment.position(newSize, bounds)
		withBounds(Bounds(newTopLeft, newSize))
	}
	
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
	def withScaledBounds(scaling: Dimensional[Double]) = withBounds(bounds * scaling)
	
	/**
	  * @param translation Translation to apply to this item's position
	  * @return A copy of this item with translated position
	  */
	def translated(translation: Dimensional[Double]) = withBounds(bounds + translation)
	
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
	def enlarged(enlargement: VectorLike[_ <: V]) =
		withBounds(Bounds(topLeft - enlargement / 2, size + enlargement))
	/**
	  * Creates a copy of this item with shrunk bounds where the center-point remains the same
	  * @param shrinking A size decrease to apply
	  * @return A copy of this item with bounds that keep the same center-point but have shrunk size
	  */
	def shrunk(shrinking: VectorLike[_ <: V]) = withBounds(Bounds(topLeft + shrinking / 2, size - shrinking))
}
package utopia.paradigm.shape.shape2d

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.shape.shape1d.Vector1D
import utopia.paradigm.shape.template.VectorLike.V
import utopia.paradigm.shape.template.Dimensional

/**
  * A common trait for models / shapes that specify a size and may be copied
  * @author Mikko Hilpinen
  * @since 15.9.2022, v1.1
  */
trait SizedLike[+Repr] extends Sized
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return This item
	  */
	def repr: Repr
	
	/**
	  * @param size A new size for this shape
	  * @return A copy of this shape with the specified size
	  */
	def withSize(size: Size): Repr
	
	
	// OTHER    -------------------------
	
	/**
	  * @param f A mapping function to apply to this item's size
	  * @return A copy of this item with mapped size
	  */
	def mapSize(f: Size => Size) = withSize(f(size))
	
	/**
	  * Scales this item by modifying its size. Preserves shape.
	  * @param scaling A scaling modifier to apply to this item's size
	  * @return A scaled copy of this item
	  */
	def withScaledSize(scaling: Double) = withSize(size * scaling)
	/**
	  * Scales this item, modifying its size. Note: Doesn't necessarily preserve shape.
	  * @param scaling A scaling modifier to apply to this item's size (different for different axes)
	  * @return A scaled copy of this item
	  */
	def withScaledSize(scaling: Dimensional[Double]) = withSize(size * scaling)
	/**
	  * Scales this item by dividing its size. Preserves shape.
	  * @param div A modifier with which this item's size is divided
	  * @return A scaled copy of this item
	  */
	def withDividedSize(div: Double) = withSize(size / div)
	/**
	  * Scales this item by dividing its size. Note: Doesn't necessarily preserve shape.
	  * @param div A modifier with which this item's size is divided (different for different axes)
	  * @return A scaled copy of this item
	  */
	def withDividedSize(div: Dimensional[Double]) = withSize(size / div)
	
	/**
	  * Creates a copy of this item with the specified length along the specified axis
	  * @param length New length for this item on one of the axes
	  * @param preserveShape Whether the shape of this item should be preserved
	  *                      (i.e. whether the perpendicular axis should be affected, also) (default = false)
	  * @return A copy of this item with the specified length along the specified axis
	  */
	def withLength(length: Vector1D, preserveShape: Boolean = false) = {
		if (preserveShape) {
			val myLength = lengthAlong(length.axis)
			if (myLength == 0) repr else withScaledSize(length / myLength)
		}
		else
			withSize(size.withDimension(length))
	}
	/**
	  * Creates a copy of this item with the specified width
	  * @param width New width for this item
	  * @param preserveShape Whether the shape of this item should be preserved
	  *                      (i.e. whether the height of this item should be affected, also) (default = false)
	  * @return A copy of this item with the specified width
	  */
	def withWidth(width: Double, preserveShape: Boolean = false) = withLength(X(width), preserveShape)
	/**
	  * Creates a copy of this item with the specified height
	  * @param height New height for this item
	  * @param preserveShape Whether the shape of this item should be preserved
	  *                      (i.e. whether the width of this item should be affected, also) (default = false)
	  * @return A copy of this item with the specified width
	  */
	def withHeight(height: Double, preserveShape: Boolean = false) = withLength(Y(height), preserveShape)
	
	/**
	  * Returns a copy of this items that fills the specified area. Preserves shape.
	  * @param minArea The size of the area this item must be able to fill
	  * @param minimize Whether this item's size should be minimized so that it just barely fills the specified area
	  *                 (default = false)
	  * @return A copy of this item that fills the specified area but has the same shape.
	  */
	def filling(minArea: Vector2DLike[_ <: V], minimize: Boolean = false) = {
		// Case: Zero size => can't scale
		if (size.isZero)
			repr
		// Case: Scaling is required
		else if (minimize || !fills(minArea))
			withScaledSize((minArea / size).maxDimension)
		// Case: No scaling is required
		else
			repr
	}
	/**
	  * Returns a copy of this item that fits into the specified area. Preserves shape.
	  * @param maxArea The size of the area this item must fit within
	  * @param maximize Whether this item's size should be maximized within the specified area,
	  *                 so that it barely fits within that area (default = false)
	  * @return A copy of this item that fits the specified area but has the same shape
	  */
	def fittingWithin(maxArea: Vector2DLike[_ <: V], maximize: Boolean = false) = {
		// Case: Zero size => already fits
		if (size.isZero)
			repr
		// Case: Scaling is required
		else if (maximize || !fitsWithin(maxArea))
			withScaledSize((maxArea / size).minDimension)
		// Case: No scaling is required
		else
			repr
	}
	
	/**
	  * Returns a copy of this item that spans at least the specified length. Preserves shape.
	  * @param minLength The smallest allowed length for this item on some axis
	  * @param minimize Whether this item should be minimized to barely span the specified length (default = false)
	  * @return A scaled copy of this item that spans at least the specified length
	  */
	def spanning(minLength: Vector1D, minimize: Boolean = false) = {
		if (minimize)
			withLength(minLength)
		else if (spans(minLength))
			repr
		else
			withLength(minLength, preserveShape = true)
	}
	/**
	  * Returns a copy of this item that fits within the specified length limit on one axis. Preserves shape.
	  * @param maxLength The largest allowed length for this item on some axis
	  * @return A scaled copy of this item that is at most the specified maximum length on the targeted axis
	  */
	def fittingWithin(maxLength: Vector1D) =
		if (fitsWithin(maxLength)) repr else withLength(maxLength, preserveShape = true)
	/**
	  * Creates a copy of this item that is of at least the specified width. Preserves shape.
	  * @param minWidth The smallest allowed width
	  * @param minimize Whether the width of this item should be set to the specified minimum width
	  * @return A copy of this item with at least the specified width, but same shape
	  */
	def spanningWidth(minWidth: Double, minimize: Boolean = false) = spanning(X(minWidth), minimize)
	/**
	  * Creates a copy of this item that is of at least the specified height. Preserves shape.
	  * @param minHeight The smallest allowed height
	  * @param minimize Whether the height of this item should be set to the specified minimum height
	  * @return A copy of this item with at least the specified height, but same shape
	  */
	def spanningHeight(minHeight: Double, minimize: Boolean = false) = spanning(Y(minHeight), minimize)
	/**
	  * Creates a copy of this item that fits within the specified width limit. Preserves shape.
	  * @param maxWidth The largest allowed width
	  * @param maximize Whether the width of this item should be set to the specified maximum width
	  * @return A copy of this item fitting to the specified width limit, with its shape preserved
	  */
	def fittingWithinWidth(maxWidth: Double, maximize: Boolean = false) = _fittingWithin(X(maxWidth), maximize)
	/**
	  * Creates a copy of this item that fits within the specified height limit. Preserves shape.
	  * @param maxHeight The largest allowed height
	  * @param maximize Whether the height of this item should be set to the specified maximum height
	  * @return A copy of this item fitting to the specified height limit, with its shape preserved
	  */
	def fittingWithinHeight(maxHeight: Double, maximize: Boolean = false) = _fittingWithin(Y(maxHeight), maximize)
	
	/**
	  * Creates a copy of this item that has its size reduced to fit within the specified maximum threshold.
	  * Doesn't preserve shape.
	  * @param maxArea The size of the area this item must fit within
	  * @return A copy of this area that fits the specified area, not necessarily having the same shape.
	  *         If this item already fit within the specified area, returns this area.
	  */
	def croppedToFitWithin(maxArea: Vector2DLike[_ <: V]) = withSize(size.combineWith(maxArea) { _ min _ })
	/**
	  * Creates a copy of this item that has its size reduced to fit within the specified maximum length threshold.
	  * Doesn't preserve shape.
	  * @param maxLength A vector that determines a maximum length for this item on a specific axis
	  * @return A copy of this area that fits within the specified maximum length, not necessarily having the same shape.
	  *         If this item already fits within the specified length, returns this area.
	  */
	def croppedToFitWithin(maxLength: Vector1D) = withSize(size.mapAxis(maxLength.axis) { _ min maxLength.length })
	/**
	  * Creates a copy of this item that has its size reduced to fit within the specified maximum width threshold.
	  * Doesn't preserve shape.
	  * @param maxWidth The largest allowed width for this item
	  * @return A copy of this area that fits within the specified maximum width limit,
	  *         not necessarily having the same shape.
	  *         If this item already fits within the specified limit, returns this area.
	  */
	def croppedHorizontallyWithin(maxWidth: Double) = croppedToFitWithin(X(maxWidth))
	/**
	  * Creates a copy of this item that has its size reduced to fit within the specified maximum height threshold.
	  * Doesn't preserve shape.
	  * @param maxHeight The largest allowed height for this item
	  * @return A copy of this area that fits within the specified maximum height limit,
	  *         not necessarily having the same shape.
	  *         If this item already fits within the specified limit, returns this area.
	  */
	def croppedVerticallyWithin(maxHeight: Double) = croppedToFitWithin(Y(maxHeight))
	
	private def _fittingWithin(length: Vector1D, maximize: Boolean) =
		if (maximize) withLength(length) else fittingWithin(length)
}

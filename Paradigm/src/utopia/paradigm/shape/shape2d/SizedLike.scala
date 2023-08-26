package utopia.paradigm.shape.shape2d

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.shape1d.Dimension
import utopia.paradigm.shape.template.{HasDimensions, NumericVectorFactory, NumericVectorLike}

import scala.math.Ordering.Implicits.infixOrderingOps

/**
  * A common trait for models / shapes that specify some kind of size and which may be copied
  * @author Mikko Hilpinen
  * @since 25.8.2023, v1.4
  */
trait SizedLike[D, S <: NumericVectorLike[D, S, _], +Repr] extends HasSizeLike[D, S]
{
	// ABSTRACT ---------------------------
	
	/**
	  * @return This item
	  */
	def self: Repr
	
	/**
	  * @return Factory used for constructing new size instances
	  */
	protected def sizeFactory: NumericVectorFactory[D, S]
	
	/**
	  * @param size A new size for this shape
	  * @return A copy of this shape with the specified size
	  */
	def withSize(size: S): Repr
	
	
	// OTHER    -------------------------
	
	/**
	  * @param f A mapping function to apply to this item's size
	  * @return A copy of this item with mapped size
	  */
	def mapSize(f: S => S) = withSize(f(size))
	
	/**
	  * Scales this item by modifying its size. Preserves shape.
	  * @param scaling A scaling modifier to apply to this item's size
	  * @return A scaled copy of this item
	  */
	def withScaledSize(scaling: D) = withSize(size * scaling)
	/**
	  * Scales this item, modifying its size. Note: Doesn't necessarily preserve shape.
	  * @param scaling A scaling modifier to apply to this item's size (different for different axes)
	  * @return A scaled copy of this item
	  */
	def withScaledSize(scaling: HasDimensions[D]) = withSize(size * scaling)
	/**
	  * Scales this item by dividing its size. Preserves shape.
	  * @param div A modifier with which this item's size is divided
	  * @return A scaled copy of this item
	  */
	def withDividedSize(div: D) = withSize(size / div)
	/**
	  * Scales this item by dividing its size. Note: Doesn't necessarily preserve shape.
	  * @param div A modifier with which this item's size is divided (different for different axes)
	  * @return A scaled copy of this item
	  */
	def withDividedSize(div: HasDimensions[D]) = withSize(size / div)
	
	/**
	  * Creates a copy of this item with the specified length along the specified axis
	  * @param length New length for this item on one of the axes
	  * @param preserveShape Whether the shape of this item should be preserved
	  *                      (i.e. whether the perpendicular axis should be affected, also) (default = false)
	  * @return A copy of this item with the specified length along the specified axis
	  */
	def withLength(length: Dimension[D], preserveShape: Boolean = false) = {
		if (preserveShape) {
			length.axis match {
				case axis: Axis2D =>
					val myLength = lengthAlong(axis)
					if (myLength == n.zero)
						self
					else {
						val scaling = n.toDouble(length.value) / n.toDouble(myLength)
						withSize(sizeFactory(
							length.value, sizeFactory.scale(lengthAlong(axis.perpendicular), scaling), axis))
					}
				case _ => self
			}
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
	def withWidth(width: D, preserveShape: Boolean = false) =
		withLength(Dimension(X, width, n.zero), preserveShape)
	/**
	  * Creates a copy of this item with the specified height
	  * @param height New height for this item
	  * @param preserveShape Whether the shape of this item should be preserved
	  *                      (i.e. whether the width of this item should be affected, also) (default = false)
	  * @return A copy of this item with the specified width
	  */
	def withHeight(height: D, preserveShape: Boolean = false) =
		withLength(Dimension(Y, height, n.zero), preserveShape)
	
	/**
	  * Returns a copy of this items that fills the specified area. Preserves shape.
	  * @param minArea The size of the area this item must be able to fill
	  * @param minimize Whether this item's size should be minimized so that it just barely fills the specified area
	  *                 (default = false)
	  * @return A copy of this item that fills the specified area but has the same shape.
	  */
	def filling[V <: NumericVectorLike[D, V, _]](minArea: V, minimize: Boolean = false) = {
		// Case: Zero size => can't scale
		if (size.isZero)
			self
		// Case: Scaling is required
		else if (minimize || !fills(minArea))
			withScaledSize((minArea / size).maxDimension)
		// Case: No scaling is required
		else
			self
	}
	/**
	  * Returns a copy of this item that fits into the specified area. Preserves shape.
	  * @param maxArea The size of the area this item must fit within
	  * @param maximize Whether this item's size should be maximized within the specified area,
	  *                 so that it barely fits within that area (default = false)
	  * @return A copy of this item that fits the specified area but has the same shape
	  */
	def fittingWithin[V <: NumericVectorLike[D, V, _]](maxArea: V, maximize: Boolean = false) = {
		// Case: Zero size => already fits
		if (size.isZero)
			self
		// Case: Scaling is required
		else if (maximize || !fitsWithin(maxArea))
			withScaledSize((maxArea / size).minDimension)
		// Case: No scaling is required
		else
			self
	}
	
	/**
	  * Returns a copy of this item that spans at least the specified length. Preserves shape.
	  * @param minLength The smallest allowed length for this item on some axis
	  * @param minimize Whether this item should be minimized to barely span the specified length (default = false)
	  * @return A scaled copy of this item that spans at least the specified length
	  */
	def spanning(minLength: Dimension[D], minimize: Boolean = false) = {
		if (minimize || !spans(minLength))
			withLength(minLength, preserveShape = true)
		else
			self
	}
	/**
	  * Returns a copy of this item that fits within the specified length limit on one axis. Preserves shape.
	  * @param maxLength The largest allowed length for this item on some axis
	  * @return A scaled copy of this item that is at most the specified maximum length on the targeted axis
	  */
	def fittingWithin(maxLength: Dimension[D]) =
		if (fitsWithin(maxLength)) self else withLength(maxLength, preserveShape = true)
	/**
	  * Creates a copy of this item that is of at least the specified width. Preserves shape.
	  * @param minWidth The smallest allowed width
	  * @param minimize Whether the width of this item should be set to the specified minimum width
	  * @return A copy of this item with at least the specified width, but same shape
	  */
	def spanningWidth(minWidth: D, minimize: Boolean = false) = spanning(Dimension(X, minWidth, n.zero), minimize)
	/**
	  * Creates a copy of this item that is of at least the specified height. Preserves shape.
	  * @param minHeight The smallest allowed height
	  * @param minimize Whether the height of this item should be set to the specified minimum height
	  * @return A copy of this item with at least the specified height, but same shape
	  */
	def spanningHeight(minHeight: D, minimize: Boolean = false) =
		spanning(Dimension(Y, minHeight, n.zero), minimize)
	/**
	  * Creates a copy of this item that fits within the specified width limit. Preserves shape.
	  * @param maxWidth The largest allowed width
	  * @param maximize Whether the width of this item should be set to the specified maximum width
	  * @return A copy of this item fitting to the specified width limit, with its shape preserved
	  */
	def fittingWithinWidth(maxWidth: D, maximize: Boolean = false) =
		_fittingWithin(Dimension(X, maxWidth, n.zero), maximize)
	/**
	  * Creates a copy of this item that fits within the specified height limit. Preserves shape.
	  * @param maxHeight The largest allowed height
	  * @param maximize Whether the height of this item should be set to the specified maximum height
	  * @return A copy of this item fitting to the specified height limit, with its shape preserved
	  */
	def fittingWithinHeight(maxHeight: D, maximize: Boolean = false) =
		_fittingWithin(Dimension(Y, maxHeight, n.zero), maximize)
	
	/**
	  * Creates a copy of this item that has its size reduced to fit within the specified maximum threshold.
	  * Doesn't preserve shape.
	  * @param maxArea The size of the area this item must fit within
	  * @return A copy of this area that fits the specified area, not necessarily having the same shape.
	  *         If this item already fit within the specified area, returns this area.
	  */
	def croppedToFitWithin(maxArea: HasDimensions[D]) = withSize(size.mergeWith(maxArea) { _ min _ })
	/**
	  * Creates a copy of this item that has its size reduced to fit within the specified maximum length threshold.
	  * Doesn't preserve shape.
	  * @param maxLength A vector that determines a maximum length for this item on a specific axis
	  * @return A copy of this area that fits within the specified maximum length, not necessarily having the same shape.
	  *         If this item already fits within the specified length, returns this area.
	  */
	def croppedToFitWithin(maxLength: Dimension[D]) =
		withSize(size.mapDimension(maxLength.axis) { _ min maxLength.value })
	/**
	  * Creates a copy of this item that has its size reduced to fit within the specified maximum width threshold.
	  * Doesn't preserve shape.
	  * @param maxWidth The largest allowed width for this item
	  * @return A copy of this area that fits within the specified maximum width limit,
	  *         not necessarily having the same shape.
	  *         If this item already fits within the specified limit, returns this area.
	  */
	def croppedHorizontallyWithin(maxWidth: D) = croppedToFitWithin(Dimension(X, maxWidth, n.zero))
	/**
	  * Creates a copy of this item that has its size reduced to fit within the specified maximum height threshold.
	  * Doesn't preserve shape.
	  * @param maxHeight The largest allowed height for this item
	  * @return A copy of this area that fits within the specified maximum height limit,
	  *         not necessarily having the same shape.
	  *         If this item already fits within the specified limit, returns this area.
	  */
	def croppedVerticallyWithin(maxHeight: D) = croppedToFitWithin(Dimension(Y, maxHeight, n.zero))
	
	private def _fittingWithin(length: Dimension[D], maximize: Boolean) =
		if (maximize) withLength(length) else fittingWithin(length)
}

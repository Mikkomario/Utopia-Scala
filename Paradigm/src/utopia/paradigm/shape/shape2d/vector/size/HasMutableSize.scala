package utopia.paradigm.shape.shape2d.vector.size

import utopia.paradigm.enumeration.Axis.{X, Y}
import utopia.paradigm.enumeration.Axis2D
import utopia.paradigm.shape.template.HasDimensions.HasDoubleDimensions

/**
  * Common trait for items which have a mutable size property
  * @author Mikko Hilpinen
  * @since 07/02/2024, v1.5.1
  */
trait HasMutableSize extends HasSize
{
	// ABSTRACT -----------------------
	
	def size_=(newSize: Size): Unit
	
	
	// COMPUTED -----------------------
	
	def width_=(newWidth: Double) = size = size.withWidth(newWidth)
	def height_=(newHeight: Double) = size = size.withHeight(newHeight)
	
	
	// OTHER    -----------------------
	
	/**
	  * Scales the size of this item
	  * @param mod Size scaling factor
	  */
	def *=(mod: Double) = size *= mod
	/**
	  * Scales the size of this item
	  * @param mod Size scaling factor
	  */
	def *=(mod: HasDoubleDimensions) = size *= mod
	/**
	  * Divides the size of this item
	  * @param div Size divider
	  */
	def /=(div: Double) = size /= div
	
	/**
	  * Updates the size of this item
	  * @param newSize New size for this item
	  * @param preserveShape Whether item shape (i.e. width-to-height ratio) should be preserved (default = false)
	  */
	def resize(newSize: Size, preserveShape: Boolean = false) = {
		if (preserveShape)
			*=((newSize.width / width) min (newSize.height / height))
		else
			size = newSize
	}
	
	/**
	  * Resizes this item to exactly fill the specified area. Preserves shape, though, which may cause this item to
	  * expand over the area along one axis
	  * @param area Area to fill
	  */
	def resizeToFill(area: Size) = if (size.nonZero) *=((area / size).xyPair.max)
	/**
	  * Resizes this item to exactly fit the specified area. Preserves shape, though, which may cause this item to
	  * shrink inside the area along one axis
	  * @param area Area to fit to
	  */
	def resizeToFit(area: Size) = if (size.nonZero) *=((area / size).xyPair.min)
	/**
	  * Makes sure this item fills the specified area. If this item is already larger than the area, does nothing
	  * @param area Area to fill
	  */
	def expandToFill(area: Size) = if (!area.fitsWithin(size)) resizeToFill(area)
	/**
	  * Makes sure this item fits into the specified area. If this item is already smaller than the area, does nothing
	  * @param area Area to fit into
	  */
	def shrinkToFit(area: Size) = if (!size.fitsWithin(area)) resizeToFit(area)
	
	/**
	  * Places a limitation upon either item width or height.
	  * @param side Targeted axis
	  * @param maxLength Length limitation for that axis
	  * @param preserveShape Whether the width-to-height ratio of this item should be preserved (default = false)
	  */
	def limitAlong(side: Axis2D, maxLength: Double, preserveShape: Boolean = false) = {
		if (size(side) > maxLength) {
			val limitedSize = size.withDimension(side(maxLength))
			if (preserveShape)
				shrinkToFit(limitedSize)
			else
				size = limitedSize
		}
	}
	/**
	  * Places a width limitation for this item.
	  * @param maxWidth Maximum allowed width
	  * @param preserveShape Whether the width-to-height ratio of this item should be preserved (default = false)
	  */
	def limitWidth(maxWidth: Double, preserveShape: Boolean = false) = limitAlong(X, maxWidth, preserveShape)
	/**
	  * Places a height limitation for this item.
	  * @param maxHeight Maximum allowed height.
	  * @param preserveShape Whether the width-to-height ratio of this item should be preserved (default = false)
	  */
	def limitHeight(maxHeight: Double, preserveShape: Boolean = false) = limitAlong(Y, maxHeight, preserveShape)
}

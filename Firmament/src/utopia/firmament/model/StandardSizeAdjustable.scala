package utopia.firmament.model

import utopia.firmament.model.enumeration.SizeCategory
import utopia.firmament.model.enumeration.SizeCategory.{Large, Small, VeryLarge, VerySmall}
import utopia.paradigm.transform.{Adjustment, LinearSizeAdjustable}

/**
  * Common trait for objects that define a standard size, and which can create copies of themselves with different sizes
  * @author Mikko Hilpinen
  * @since 4.5.2023, v1.1
  */
trait StandardSizeAdjustable[+Repr] extends LinearSizeAdjustable[Repr]
{
	// ABSTRACT ----------------------
	
	/**
	  * @return The relation between this item's size and the standard size for this item.
	  *         E.g. If this item is of the standard size, returns 1.0.
	  *         If this item is 20% larger than the standard size, returns 1.2.
	  */
	protected def relativeToStandardSize: Double
	
	
	// COMPUTED ---------------------
	
	/**
	  * @return A copy of this item that has the standard size
	  */
	def standardSized = this / relativeToStandardSize
	
	/**
	  * @param adj Implicit adjustment levels
	  * @return A very small copy of this item (relative to standard size)
	  */
	def verySmall(implicit adj: Adjustment) = apply(VerySmall)
	/**
	  * @param adj Implicit adjustment levels
	  * @return A small copy of this item (relative to standard size)
	  */
	def small(implicit adj: Adjustment) = apply(Small)
	/**
	  * @param adj Implicit adjustment levels
	  * @return A large copy of this item (relative to standard size)
	  */
	def large(implicit adj: Adjustment) = apply(Large)
	/**
	  * @param adj Implicit adjustment levels
	  * @return A very large copy of this item (relative to standard size)
	  */
	def veryLarge(implicit adj: Adjustment) = apply(VeryLarge)
	
	
	// OTHER    ---------------------
	
	/**
	  * @param size New item size
	  * @param adj Adjustment levels used (implicit)
	  * @return A copy of this item with the specified size
	  */
	def apply(size: SizeCategory)(implicit adj: Adjustment) = this * (size.scaling / relativeToStandardSize)
}

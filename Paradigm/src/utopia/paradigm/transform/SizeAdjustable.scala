package utopia.paradigm.transform

/**
  * Common trait for items that may be converted into larger or smaller versions
  * @author Mikko Hilpinen
  * @since 3.5.2023, v1.3.1
  */
trait SizeAdjustable[+Repr] extends Any
{
	// ABSTRACT -------------------
	
	/**
	  * @param impact The level of impact applied, where 0 is no change, 1 is larger and -1 is smaller
	  * @param adjustment Adjustment scaling to apply
	  * @return A scaled / adjusted copy of this item
	  */
	protected def adjustedBy(impact: Int)(implicit adjustment: Adjustment): Repr
	
	
	// COMPUTED -------------------
	
	/**
	  * @param adj Implicit adjustment scaling factors to use
	  * @return A copy of this item that is much smaller
	  */
	def muchSmaller(implicit adj: Adjustment) = adjustedBy(-2)
	/**
	  * @param adj Implicit adjustment scaling factors to use
	  * @return A copy of this item that is a step smaller
	  */
	def smaller(implicit adj: Adjustment) = adjustedBy(-1)
	/**
	  * @param adj Implicit adjustment scaling factors to use
	  * @return A copy of this item that is a step larger
	  */
	def larger(implicit adj: Adjustment) = adjustedBy(1)
	/**
	  * @param adj Implicit adjustment scaling factors to use
	  * @return A copy of this item that is much larger
	  */
	def muchLarger(implicit adj: Adjustment) = adjustedBy(2)
}

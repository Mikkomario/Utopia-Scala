package utopia.paradigm.transform

import utopia.flow.operator.LinearScalable

/**
  * Common trait for items that may be converted into larger or smaller versions
  * @author Mikko Hilpinen
  * @since 3.5.2023, v1.3.1
  */
trait SizeAdjustable[+Repr] extends Any with LinearScalable[Repr]
{
	// COMPUTED -------------------
	
	/**
	  * @param adj Implicit adjustment scaling factors to use
	  * @return A copy of this item that is much smaller
	  */
	def muchSmaller(implicit adj: Adjustment) = this * adj(-2)
	/**
	  * @param adj Implicit adjustment scaling factors to use
	  * @return A copy of this item that is a step smaller
	  */
	def smaller(implicit adj: Adjustment) = this * adj(-1)
	/**
	  * @param adj Implicit adjustment scaling factors to use
	  * @return A copy of this item that is a step larger
	  */
	def larger(implicit adj: Adjustment) = this * adj(1)
	/**
	  * @param adj Implicit adjustment scaling factors to use
	  * @return A copy of this item that is much larger
	  */
	def muchLarger(implicit adj: Adjustment) = this * adj(2)
}

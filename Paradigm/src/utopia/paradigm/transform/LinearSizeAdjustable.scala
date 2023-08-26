package utopia.paradigm.transform

import utopia.flow.operator.LinearScalable

/**
  * Common trait for items that may be converted into larger or smaller versions
  * @author Mikko Hilpinen
  * @since 3.5.2023, v1.3.1
  */
trait LinearSizeAdjustable[+Repr] extends Any with LinearScalable[Repr] with SizeAdjustable[Repr]
{
	override protected def adjustedBy(impact: Int)(implicit adjustment: Adjustment): Repr = this * adjustment(impact)
}

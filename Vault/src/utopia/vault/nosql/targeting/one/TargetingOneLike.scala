package utopia.vault.nosql.targeting.one

import utopia.flow.generic.casting.ValueConversions._
import utopia.flow.generic.model.immutable.Value
import utopia.vault.nosql.targeting.TargetingLike

/**
  * Common trait for access points that yield individual items with each query
  * @author Mikko Hilpinen
  * @since 19.05.2025, v1.21
  */
trait TargetingOneLike[+A, +Repr] extends TargetingLike[A, Value, Option[Seq[Value]], Repr]
{
	/**
	  * @param id Id / primary key of the targeted item
	  * @return Access to the item with the specified primary key
	  */
	def apply(id: Int): Repr = apply(index <=> id)
}
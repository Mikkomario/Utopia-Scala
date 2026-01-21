package utopia.vault.nosql.targeting.grouped

import utopia.flow.generic.model.immutable.Value
import utopia.vault.nosql.targeting.TargetingLike
import utopia.vault.nosql.targeting.columns.AccessManyColumns
import utopia.vault.nosql.view.ViewManyByIntIds

/**
  * Common trait for access points that parse items from multiple rows at a time
  * @author Mikko Hilpinen
  * @since 20.01.2026, v2.1
  */
trait TargetingGroupedLike[+A, +Repr]
	extends TargetingLike[A, Seq[Value], Seq[Seq[Value]], Repr] with AccessManyColumns with ViewManyByIntIds[Repr]
